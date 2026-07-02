package com.example.parkinglot.sdk.model.responses;

import com.example.parkinglot.sdk.model.EndpointDiagnostics;
import com.example.parkinglot.sdk.model.LogEntry;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DiagnosticsResponse {
    private Instant startedAt;
    private long uptimeMillis;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private Map<String, EndpointDiagnostics> endpoints = new HashMap<>();
    private List<LogEntry> recentLogs = new ArrayList<>();
}

