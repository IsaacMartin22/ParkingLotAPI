package com.example.apiservice.dto;

import java.time.Instant;
import java.util.Map;

public record DiagnosticsResponse(
        Instant startedAt,
        long uptimeMillis,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        Map<String, EndpointDiagnostics> endpoints
) {
}
