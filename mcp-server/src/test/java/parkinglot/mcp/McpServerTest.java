package parkinglot.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mcp-analytics-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.hikari.read-only=false",
                "spring.datasource.hikari.data-source-properties.readOnlyMode="
        }
)
@ExtendWith(OutputCaptureExtension.class)
class McpServerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedAnalytics() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS analytics");
        jdbcTemplate.execute("""
                CREATE TABLE analytics (
                    id BIGSERIAL PRIMARY KEY,
                    current_url VARCHAR(255),
                    browser VARCHAR(255),
                    operating_system VARCHAR(255),
                    ip_address VARCHAR(255),
                    "timestamp" TIMESTAMP,
                    payload JSON,
                    event_type VARCHAR(255),
                    session_id VARCHAR(255)
                )
                """);
        jdbcTemplate.execute("""
                INSERT INTO analytics (current_url, browser, operating_system, ip_address, "timestamp", event_type, session_id) VALUES
                ('https://example.com/', 'Chrome', 'Windows', '10.0.0.1', TIMESTAMP '2026-10-01 10:00:00', 'PAGE_VIEW', 's1'),
                ('https://example.com/projects', 'Chrome', 'Windows', '10.0.0.1', TIMESTAMP '2026-10-01 10:05:00', 'PAGE_VIEW', 's1'),
                ('https://example.com/projects', 'Firefox', 'Linux', '10.0.0.2', TIMESTAMP '2026-10-02 12:00:00', 'PAGE_VIEW', 's2'),
                ('https://example.com/projects', 'Firefox', 'Linux', '10.0.0.2', TIMESTAMP '2026-10-02 12:01:00', 'CLICK', 's2'),
                ('https://example.com/', 'Safari', 'macOS', '10.0.0.3', TIMESTAMP '2026-10-05 08:00:00', 'ERROR', 's3')
                """);
    }

    @Test
    void discoversToolsAndValidatesPortfolioSearchOverHttp() {
        try (McpSyncClient client = client()) {
            assertEquals("parking-lot-mcp-server", client.initialize().serverInfo().name());
            List<McpSchema.Tool> tools = client.listTools().tools();
            assertEquals(List.of(SiteAnalyticsTool.NAME, PortfolioSearchTool.NAME),
                    tools.stream().map(McpSchema.Tool::name).sorted().toList());

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(PortfolioSearchTool.NAME, Map.of()));
            assertTrue(Boolean.TRUE.equals(result.isError()));
            assertTrue(text(result).contains("errorCode=INVALID_ARGUMENTS"));
        }
    }

    @Test
    void returnsAggregateAnalyticsWithoutVisitorIdentifiers() throws Exception {
        try (McpSyncClient client = client()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    SiteAnalyticsTool.NAME,
                    Map.of("eventType", "PAGE_VIEW", "groupBy", "currentUrl", "endTime", "2026-10-02")
            ));

            assertFalse(Boolean.TRUE.equals(result.isError()), text(result));
            JsonNode json = objectMapper.readTree(text(result));
            assertEquals(3, json.path("totalEvents").asLong());
            assertEquals(2, json.path("uniqueSessions").asLong());
            assertEquals("https://example.com/projects", json.path("groups").path(0).path("currentUrl").asText());
            assertEquals(2, json.path("groups").path(0).path("events").asLong());
            assertEquals(1, json.path("groups").path(1).path("events").asLong());
            assertFalse(text(result).contains("10.0.0."));
            assertFalse(text(result).contains("\"s1\""));
        }
    }

    @Test
    void filtersByUrlAndGroupsByDay() throws Exception {
        try (McpSyncClient client = client()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    SiteAnalyticsTool.NAME,
                    Map.of("urlContains", "PROJECTS", "groupBy", "day", "limit", 1)
            ));

            JsonNode json = objectMapper.readTree(text(result));
            assertEquals(3, json.path("totalEvents").asLong());
            assertEquals(1, json.path("groups").size());
            assertEquals("2026-10-02", json.path("groups").path(0).path("day").asText());
        }
    }

    @Test
    void rejectsInvalidArgumentsAsToolErrors(CapturedOutput output) {
        try (McpSyncClient client = client()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    SiteAnalyticsTool.NAME, Map.of("groupBy", "ip_address")
            ));

            assertTrue(Boolean.TRUE.equals(result.isError()));
            assertTrue(text(result).startsWith("Invalid arguments"));
            assertTrue(text(result).contains("errorCode=INVALID_ARGUMENTS"));
            assertTrue(output.getAll().contains(errorId(result)));
            assertTrue(output.getAll().contains("MCP tool call rejected"));
        }
    }

    @Test
    void logsDatabaseFailureWithCorrelatedErrorAndNoArgumentValues(CapturedOutput output) {
        jdbcTemplate.execute("DROP TABLE analytics");
        try (McpSyncClient client = client()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    SiteAnalyticsTool.NAME, Map.of("urlContains", "private-filter-value")
            ));

            assertTrue(Boolean.TRUE.equals(result.isError()));
            assertTrue(text(result).contains("errorCode=DATABASE_ERROR"));
            assertTrue(Pattern.compile("sqlState=42S0[24]").matcher(text(result)).find(), text(result));
            assertFalse(text(result).contains("SELECT"));
            assertFalse(text(result).contains("BadSqlGrammarException"));
            assertTrue(output.getAll().contains(errorId(result)));
            assertTrue(output.getAll().contains("exceptionType=BadSqlGrammarException"));
            assertTrue(Pattern.compile("sqlState=42S0[24]").matcher(output.getAll()).find());
            assertTrue(output.getAll().contains("Caused by:"));
            assertFalse(output.getAll().contains("private-filter-value"));
        }
    }

    private static String errorId(McpSchema.CallToolResult result) {
        var matcher = Pattern.compile("errorId=([0-9a-f-]{36})\\]$").matcher(text(result));
        assertTrue(matcher.find(), text(result));
        return matcher.group(1);
    }

    private McpSyncClient client() {
        var transport = HttpClientSseClientTransport.builder("http://127.0.0.1:" + port)
                .sseEndpoint("/mcp/sse")
                .build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5)).build();
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
