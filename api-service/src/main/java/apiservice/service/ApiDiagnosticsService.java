package apiservice.service;

import parkinglot.common.model.EndpointDiagnostics;
import parkinglot.common.response.ApiDiagnosticsResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ApiDiagnosticsService implements SmartInitializingSingleton {

    private static final Set<RequestMethod> DEFAULT_HTTP_METHODS = Set.of(
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE
    );

    private static final String ERROR_ENDPOINT = "/error";

    private final Instant startedAt = Instant.now();
    private final ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMappingProvider;

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();

    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    public ApiDiagnosticsService(
            @Qualifier("requestMappingHandlerMapping")
            ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMappingProvider
    ) {
        this.requestMappingHandlerMappingProvider = requestMappingHandlerMappingProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initializeEndpoints();
    }

    private void initializeEndpoints() {
        RequestMappingHandlerMapping requestMappingHandlerMapping =
                requestMappingHandlerMappingProvider.getIfAvailable();

        if (requestMappingHandlerMapping == null) {
            return;
        }

        for (RequestMappingInfo mappingInfo : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = mappingInfo.getPatternValues();
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            Set<RequestMethod> methodsToUse = methods.isEmpty() ? DEFAULT_HTTP_METHODS : methods;

            for (String pattern : patterns) {
                for (RequestMethod method : methodsToUse) {
                    if (!pattern.equals(ERROR_ENDPOINT)) {
                        endpointMetrics.putIfAbsent(method.name() + " " + pattern, new EndpointMetrics());
                    }
                }
            }
        }
    }

    public void recordSuccess(String endpoint, long duration) {
        recordRequest(endpoint, true, duration);
    }

    public void recordFailure(String endpoint, long duration) {
        recordRequest(endpoint, false, duration);
    }

    private void recordRequest(String endpoint, boolean successful, long durationMillis) {
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

    public ApiDiagnosticsResponse getDiagnostics() {
        Map<String, EndpointDiagnostics> endpoints = endpointMetrics.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toDiagnostics(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return new ApiDiagnosticsResponse(
                startedAt,
                Duration.between(startedAt, Instant.now()).toMillis(),
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                endpoints,
                // Used to be logs, can be removed. Need to update frontend response object to remove this
                new ArrayList<>()
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
