package parkinglot.common.model;

public record EndpointDiagnostics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long totalDurationMillis,
        double averageDurationMillis
) {
}

