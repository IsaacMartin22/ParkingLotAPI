package parkinglot.common.response;

import parkinglot.common.model.LongRunningQuery;

import java.util.List;

public record MongoDatabaseDiagnosticsResponse(
        boolean connectivity,
        long latency,
        long uptimeMillis,
        long activeConnections,
        long maxConnections,
        long databaseSize,
        List<LongRunningQuery> longRunningOperations
) {
}
