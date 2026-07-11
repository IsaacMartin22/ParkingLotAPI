package apiservice.service;

import parkinglot.common.model.LongRunningQuery;
import parkinglot.common.response.DatabaseDiagnosticsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseDiagnosticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseDiagnosticsResponse getDiagnostics() {
        boolean connectivity = checkConnectivity();
        long latency = measureLatency();
        long uptimeMillis = queryUptimeMillis();
        long activeConnections = queryActiveConnections();
        long maxConnections = queryMaxConnections();
        long databaseSize = queryDatabaseSize();
        List<LongRunningQuery> longRunningQueries = queryLongRunningQueries();

        return new DatabaseDiagnosticsResponse(
                connectivity,
                latency,
                uptimeMillis,
                activeConnections,
                maxConnections,
                databaseSize,
                longRunningQueries
        );
    }

    private boolean checkConnectivity() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long measureLatency() {
        try {
            long start = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1L;
        }
    }

    private long queryUptimeMillis() {
        try {
            Double seconds = jdbcTemplate.queryForObject(
                    "SELECT EXTRACT(EPOCH FROM (now() - pg_postmaster_start_time()))",
                    Double.class
            );
            return seconds != null ? (long) (seconds * 1000) : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    private long queryActiveConnections() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity",
                    Long.class
            );
            return count != null ? count : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    private long queryMaxConnections() {
        try {
            String raw = jdbcTemplate.queryForObject("SHOW max_connections", String.class);
            return raw != null ? Long.parseLong(raw.trim()) : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    private long queryDatabaseSize() {
        try {
            Long bytes = jdbcTemplate.queryForObject(
                    "SELECT pg_database_size(current_database())",
                    Long.class
            );
            return bytes != null ? bytes : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    private List<LongRunningQuery> queryLongRunningQueries() {
        try {
            return jdbcTemplate.query(
                    """
                    SELECT query,
                           EXTRACT(EPOCH FROM (now() - query_start)) * 1000 AS duration_ms
                    FROM pg_stat_activity
                    WHERE state != 'idle'
                      AND query_start IS NOT NULL
                      AND now() - query_start > interval '5 seconds'
                    ORDER BY query_start
                    """,
                    (rs, rowNum) -> new LongRunningQuery(
                            rs.getLong("duration_ms"),
                            rs.getString("query")
                    )
            );
        } catch (Exception e) {
            return List.of();
        }
    }
}

