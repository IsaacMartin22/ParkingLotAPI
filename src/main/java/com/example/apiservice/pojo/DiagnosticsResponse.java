package com.example.apiservice.pojo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DiagnosticsResponse(
        Instant startedAt,
        long uptimeMillis,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        Map<String, EndpointDiagnostics> endpoints,
        List<LogEntry> recentLogs
) {
}
