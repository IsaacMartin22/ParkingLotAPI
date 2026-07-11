package parkinglot.common.response;

import parkinglot.common.model.LongRunningQuery;

import java.util.List;

public record DatabaseDiagnosticsResponse(
        boolean connectivity,
        long latency,
        long uptimeMillis,
        long activeConnections,
        long maxConnections,
        long databaseSize,
        List<LongRunningQuery> longRunningQueries
) {
}
/*
Connectivity	Attempt a connection or execute SELECT 1
Query latency	Measure execution time of a simple query
Uptime	SELECT now() - pg_postmaster_start_time();
Active connections	SELECT count(*) FROM pg_stat_activity;
Max connections	SHOW max_connections;
Database size	SELECT pg_size_pretty(pg_database_size(current_database()));
Long-running queries	pg_stat_activity
 */
