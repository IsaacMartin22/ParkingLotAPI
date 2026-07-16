package apiservice.repository;

import apiservice.dbentity.Analytics;
import parkinglot.common.request.AnalyticsQueryFilter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // Whitelist of sortable columns
    private static final Map<String, String> FIELD_COLUMNS = Map.of(
            "eventType", "event_type",
            "currentUrl", "current_url",
            "browser", "browser",
            "operatingSystem", "operating_system",
            "sessionId", "session_id",
            "ipAddress", "ip_address",
            "timestamp", "\"timestamp\"",
            "id", "id"
    );

    @Transactional
    public Analytics save(Analytics analytics) {
        if (analytics == null) {
            throw new IllegalArgumentException("Analytics required");
        }

        if (analytics.getId() == null) {
            entityManager.persist(analytics);
            return analytics;
        }

        return entityManager.merge(analytics);
    }

    public List<Analytics> query(
            Integer offset,
            Integer limit,
            String sortField,
            String sortDirection,
            List<AnalyticsQueryFilter> filters
    ) {

        StringBuilder sql = new StringBuilder("""
                SELECT * 
                FROM analytics 
                WHERE 1 = 1 
                """);

        int filterIndex = 0;
        for (AnalyticsQueryFilter filter : filters) {
            String column = FIELD_COLUMNS.get(filter.field());
            if (column == null) {
                continue;
            }

            String parameterName = "filter" + filterIndex++;
            sql.append(" AND ");
            appendFilterClause(sql, column, filter.operator(), parameterName, filter.field());
        }

        String column = FIELD_COLUMNS.getOrDefault(sortField, "id");

        String direction = "DESC".equalsIgnoreCase(sortDirection)
                ? "DESC"
                : "ASC";

        sql.append("""
                
                ORDER BY
                """)
                .append(column)
                .append(" ")
                .append(direction)
                .append(", id ASC");

        sql.append("""
                
                LIMIT :limit
                OFFSET :offset
                """);

        Query query = entityManager.createNativeQuery(sql.toString(), Analytics.class);

        query.setParameter("limit", limit);
        query.setParameter("offset", offset);

        filterIndex = 0;
        for (AnalyticsQueryFilter filter : filters) {
            if (FIELD_COLUMNS.containsKey(filter.field())) {
                query.setParameter("filter" + filterIndex++, filter.value());
            }
        }

        return query.getResultList();
    }

    private void appendFilterClause(
            StringBuilder sql,
            String column,
            String operator,
            String parameterName,
            String field
    ) {
        switch (operator) {
            case "eq" -> {
                sql.append(column).append(" = ");
                if ("timestamp".equals(field)) {
                    sql.append("CAST(:").append(parameterName).append(" AS timestamp)");
                } else {
                    sql.append(":").append(parameterName);
                }
            }
            case "neq" -> {
                sql.append(column).append(" <> ");
                if ("timestamp".equals(field)) {
                    sql.append("CAST(:").append(parameterName).append(" AS timestamp)");
                } else {
                    sql.append(":").append(parameterName);
                }
            }
            case "has" -> sql.append("LOWER(")
                        .append(column)
                        .append(") LIKE LOWER(CONCAT('%', :")
                        .append(parameterName)
                        .append(", '%'))");

            // Number comparison is only applicable for timestamp field
            case "lt" -> sql.append(column).append(" < CAST(:").append(parameterName).append(" AS timestamp)");
            case "lte" -> sql.append(column).append(" <= CAST(:").append(parameterName).append(" AS timestamp)");
            case "gt" -> sql.append(column).append(" > CAST(:").append(parameterName).append(" AS timestamp)");
            case "gte" -> sql.append(column).append(" >= CAST(:").append(parameterName).append(" AS timestamp)");
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
}
