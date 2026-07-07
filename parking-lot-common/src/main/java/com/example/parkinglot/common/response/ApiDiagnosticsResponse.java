package com.example.parkinglot.common.response;

import com.example.parkinglot.common.model.EndpointDiagnostics;
import com.example.parkinglot.common.model.LogEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiDiagnosticsResponse(
        Instant startedAt,
        long uptimeMillis,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        Map<String, EndpointDiagnostics> endpoints,
        List<LogEntry> recentLogs
) {
}

