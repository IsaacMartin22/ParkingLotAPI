package parkinglot.common.response;

import parkinglot.common.model.EndpointDiagnostics;
import parkinglot.common.model.HeapMemoryUsage;
import parkinglot.common.model.LogEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiDiagnosticsResponse(
        Instant startedAt,
        long uptimeMillis,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        HeapMemoryUsage heapMemoryUsage,
        Map<String, EndpointDiagnostics> endpoints,
        List<LogEntry> recentLogs
) {
}
