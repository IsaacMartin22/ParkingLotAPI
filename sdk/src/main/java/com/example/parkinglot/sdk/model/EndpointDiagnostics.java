package com.example.parkinglot.sdk.model;

import lombok.Data;

@Data
public class EndpointDiagnostics {
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalDurationMillis;
    private double averageDurationMillis;
}

