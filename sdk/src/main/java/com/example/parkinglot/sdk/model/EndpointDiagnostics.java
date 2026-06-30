package com.example.parkinglot.sdk.model;

public class EndpointDiagnostics {
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalDurationMillis;
    private double averageDurationMillis;

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSuccessfulRequests() {
        return successfulRequests;
    }

    public void setSuccessfulRequests(long successfulRequests) {
        this.successfulRequests = successfulRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(long failedRequests) {
        this.failedRequests = failedRequests;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public void setTotalDurationMillis(long totalDurationMillis) {
        this.totalDurationMillis = totalDurationMillis;
    }

    public double getAverageDurationMillis() {
        return averageDurationMillis;
    }

    public void setAverageDurationMillis(double averageDurationMillis) {
        this.averageDurationMillis = averageDurationMillis;
    }
}

