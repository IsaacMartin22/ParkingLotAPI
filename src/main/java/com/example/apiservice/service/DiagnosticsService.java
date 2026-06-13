package com.example.apiservice.service;

import com.example.apiservice.dto.DiagnosticsResponse;
import com.example.apiservice.dto.EndpointDiagnostics;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DiagnosticsService {

    private final Instant startedAt = Instant.now();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();

    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    public void recordRequest(String endpoint, int statusCode, long durationMillis) {
        boolean successful = statusCode >= 200 && statusCode < 400;

        totalRequests.incrementAndGet();

        if (successful) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }

        endpointMetrics
                .computeIfAbsent(endpoint, key -> new EndpointMetrics())
                .record(successful, durationMillis);
    }

    public DiagnosticsResponse getDiagnostics() {
        Map<String, EndpointDiagnostics> endpoints = endpointMetrics.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toDiagnostics()
                ));

        return new DiagnosticsResponse(
                startedAt,
                Duration.between(startedAt, Instant.now()).toMillis(),
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                endpoints
        );
    }

    private static class EndpointMetrics {

        private final AtomicLong totalRequests = new AtomicLong();
        private final AtomicLong successfulRequests = new AtomicLong();
        private final AtomicLong failedRequests = new AtomicLong();
        private final AtomicLong totalDurationMillis = new AtomicLong();

        void record(boolean successful, long durationMillis) {
            totalRequests.incrementAndGet();
            totalDurationMillis.addAndGet(durationMillis);

            if (successful) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
        }

        EndpointDiagnostics toDiagnostics() {
            long total = totalRequests.get();
            long duration = totalDurationMillis.get();

            return new EndpointDiagnostics(
                    total,
                    successfulRequests.get(),
                    failedRequests.get(),
                    duration,
                    total == 0 ? 0 : (double) duration / total
            );
        }
    }
}
