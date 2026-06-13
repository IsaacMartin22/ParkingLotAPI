package com.example.apiservice.dto;

public record EndpointDiagnostics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long totalDurationMillis,
        double averageDurationMillis
) {
}
