package com.example.apiservice.service;

import com.example.apiservice.pojo.DiagnosticsResponse;
import com.example.apiservice.pojo.EndpointDiagnostics;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DiagnosticsService implements SmartInitializingSingleton {

    private static final Set<RequestMethod> DEFAULT_HTTP_METHODS = Set.of(
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.HEAD
    );

    private final Instant startedAt = Instant.now();
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();

    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    public DiagnosticsService(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initializeEndpoints();
    }

    private void initializeEndpoints() {
        for (RequestMappingInfo mappingInfo : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = mappingInfo.getPatternValues();
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            Set<RequestMethod> methodsToUse = methods.isEmpty() ? DEFAULT_HTTP_METHODS : methods;

            for (String pattern : patterns) {
                for (RequestMethod method : methodsToUse) {
                    endpointMetrics.putIfAbsent(method.name() + " " + pattern, new EndpointMetrics());
                }
            }
        }
    }

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
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toDiagnostics(),
                        (left, right) -> left,
                        LinkedHashMap::new
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
