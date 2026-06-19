package com.example.apiservice.pojo;

public record EndpointDiagnostics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long totalDurationMillis,
        double averageDurationMillis
) {
}
