package parkinglot.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteAnalyticsToolTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"42P01", "42501", "08001", "28P01", "private-state\nforged-log-entry"})
    void forwardsOnlyValidSqlStateWithoutDatabaseDetails(String sqlState) {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(mock(JdbcTemplate.class));
        SiteAnalyticsTool tool = new SiteAnalyticsTool(jdbcTemplate, new ObjectMapper());
        SQLException sqlException = new SQLException("private-database-details", sqlState,
                new IOException("private-network-details"));
        doThrow(new CannotGetJdbcConnectionException("private-connection-details", sqlException))
                .when(jdbcTemplate).query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));

        McpSchema.CallToolResult result = tool.call(Map.of());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        String expectedState = sqlState != null && sqlState.matches("[0-9A-Z]{5}") ? sqlState : "unknown";

        assertTrue(Boolean.TRUE.equals(result.isError()));
        assertTrue(text.contains("errorCode=DATABASE_ERROR sqlState=" + expectedState + " errorId="), text);
        assertFalse(text.contains("private-"));
        assertFalse(text.contains("forged-log-entry"));
        assertFalse(text.contains("SELECT"));
    }
}
