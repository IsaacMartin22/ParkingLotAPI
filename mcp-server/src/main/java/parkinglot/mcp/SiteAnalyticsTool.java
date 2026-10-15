package parkinglot.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only, aggregate-only view of the analytics table. Raw rows, IP addresses, session ids and
 * payloads are never returned so the chatbot cannot leak visitor-level data.
 */
@Component
public class SiteAnalyticsTool {

    static final String NAME = "get_site_analytics";

    private static final Logger logger = LoggerFactory.getLogger(SiteAnalyticsTool.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int QUERY_TIMEOUT_SECONDS = 10;
    private static final Set<String> EVENT_TYPES = Set.of("NETWORK_SUCCESS", "PAGE_VIEW", "CLICK", "ERROR");
    private static final Map<String, String> GROUP_EXPRESSIONS = Map.of(
            "eventType", "event_type",
            "currentUrl", "current_url",
            "browser", "browser",
            "operatingSystem", "operating_system",
            "day", "CAST(\"timestamp\" AS DATE)"
    );
    private static final Set<String> ALLOWED_ARGUMENTS =
            Set.of("startTime", "endTime", "eventType", "urlContains", "groupBy", "limit");

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "startTime": {
                  "type": "string",
                  "description": "Inclusive lower bound. ISO-8601 instant (2026-10-01T00:00:00Z) or UTC date (2026-10-01)."
                },
                "endTime": {
                  "type": "string",
                  "description": "Exclusive upper bound. ISO-8601 instant, or a UTC date which includes that whole day."
                },
                "eventType": {
                  "type": "string",
                  "enum": ["NETWORK_SUCCESS", "PAGE_VIEW", "CLICK", "ERROR"],
                  "description": "Only count events of this type."
                },
                "urlContains": {
                  "type": "string",
                  "description": "Case-insensitive substring the page URL must contain."
                },
                "groupBy": {
                  "type": "string",
                  "enum": ["eventType", "currentUrl", "browser", "operatingSystem", "day"],
                  "description": "Break counts down by this dimension, highest event count first."
                },
                "limit": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 50,
                  "description": "Maximum number of groups to return (default 10)."
                }
              },
              "required": [],
              "additionalProperties": false
            }
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SiteAnalyticsTool(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcTemplate.getJdbcTemplate().setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        this.objectMapper = objectMapper;
    }

    McpServerFeatures.SyncToolSpecification specification() {
        McpSchema.Tool tool = new McpSchema.Tool(
                NAME,
                "Read-only aggregate statistics about visitors to Isaac's portfolio website, from its analytics table. "
                        + "Use for questions about site traffic, page views, clicks, errors, popular pages, browsers, "
                        + "operating systems, or activity over time. Returns total events, unique sessions, first/last "
                        + "event times (UTC), and optional grouped counts. Never returns individual visitor data.",
                INPUT_SCHEMA
        );
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> call(arguments));
    }

    McpSchema.CallToolResult call(Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        try {
            return new McpSchema.CallToolResult(objectMapper.writeValueAsString(query(args)), false);
        } catch (IllegalArgumentException ex) {
            return new McpSchema.CallToolResult("Invalid arguments: " + ex.getMessage(), true);
        } catch (JsonProcessingException | RuntimeException ex) {
            logger.error("{} failed for arguments={}", NAME, args, ex);
            return new McpSchema.CallToolResult("Site analytics are unavailable right now.", true);
        }
    }

    private Map<String, Object> query(Map<String, Object> args) {
        for (String key : args.keySet()) {
            if (!ALLOWED_ARGUMENTS.contains(key)) {
                throw new IllegalArgumentException("unknown argument '" + key + "'");
            }
        }

        Instant start = parseTime(stringArg(args, "startTime"), "startTime", false);
        Instant end = parseTime(stringArg(args, "endTime"), "endTime", true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        String eventType = stringArg(args, "eventType");
        if (eventType != null && !EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("eventType must be one of " + EVENT_TYPES);
        }
        String urlContains = stringArg(args, "urlContains");
        String groupBy = stringArg(args, "groupBy");
        if (groupBy != null && !GROUP_EXPRESSIONS.containsKey(groupBy)) {
            throw new IllegalArgumentException("groupBy must be one of " + GROUP_EXPRESSIONS.keySet());
        }
        int limit = limitArg(args.get("limit"));

        StringBuilder where = new StringBuilder(" FROM analytics WHERE 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (start != null) {
            where.append(" AND \"timestamp\" >= :start");
            params.addValue("start", Timestamp.from(start));
        }
        if (end != null) {
            where.append(" AND \"timestamp\" < :end");
            params.addValue("end", Timestamp.from(end));
        }
        if (eventType != null) {
            where.append(" AND event_type = :eventType");
            params.addValue("eventType", eventType);
        }
        if (urlContains != null) {
            where.append(" AND LOWER(current_url) LIKE :urlPattern");
            params.addValue("urlPattern", "%" + escapeLike(urlContains.toLowerCase(Locale.ROOT)) + "%");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("startTime", start == null ? null : start.toString());
        filters.put("endTime", end == null ? null : end.toString());
        filters.put("eventType", eventType);
        filters.put("urlContains", urlContains);
        result.put("filters", filters);

        jdbcTemplate.query(
                "SELECT COUNT(*) AS total_events, COUNT(DISTINCT session_id) AS unique_sessions, "
                        + "MIN(\"timestamp\") AS first_event, MAX(\"timestamp\") AS last_event" + where,
                params,
                rs -> {
                    result.put("totalEvents", rs.getLong("total_events"));
                    result.put("uniqueSessions", rs.getLong("unique_sessions"));
                    result.put("firstEventAt", toIso(rs.getTimestamp("first_event")));
                    result.put("lastEventAt", toIso(rs.getTimestamp("last_event")));
                }
        );

        if (groupBy != null) {
            String expression = GROUP_EXPRESSIONS.get(groupBy);
            params.addValue("limit", limit);
            List<Map<String, Object>> groups = new ArrayList<>(jdbcTemplate.query(
                    "SELECT " + expression + " AS group_key, COUNT(*) AS events, "
                            + "COUNT(DISTINCT session_id) AS unique_sessions" + where
                            + " GROUP BY " + expression
                            + " ORDER BY events DESC, group_key ASC LIMIT :limit",
                    params,
                    (rs, rowNum) -> {
                        Object key = rs.getObject("group_key");
                        Map<String, Object> group = new LinkedHashMap<>();
                        group.put(groupBy, key == null ? "(unknown)" : key.toString());
                        group.put("events", rs.getLong("events"));
                        group.put("uniqueSessions", rs.getLong("unique_sessions"));
                        return group;
                    }
            ));
            result.put("groupBy", groupBy);
            result.put("groups", groups);
        }
        return result;
    }

    private static String stringArg(Map<String, Object> args, String name) {
        Object value = args.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException(name + " is too long");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int limitArg(Object value) {
        if (value == null) {
            return DEFAULT_LIMIT;
        }
        if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())) {
            throw new IllegalArgumentException("limit must be an integer");
        }
        long limit = number.longValue();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return (int) limit;
    }

    private static Instant parseTime(String value, String name, boolean endOfDayForDates) {
        if (value == null) {
            return null;
        }
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value);
                return (endOfDayForDates ? date.plusDays(1) : date).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 instant or YYYY-MM-DD date");
        }
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
