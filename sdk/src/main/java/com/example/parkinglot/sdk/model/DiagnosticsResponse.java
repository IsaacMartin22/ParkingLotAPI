package com.example.parkinglot.sdk.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DiagnosticsResponse {
    private Instant startedAt;
    private long uptimeMillis;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private Map<String, EndpointDiagnostics> endpoints = new HashMap<>();

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }

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

    public Map<String, EndpointDiagnostics> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointDiagnostics> endpoints) {
        this.endpoints = endpoints;
    }
}

