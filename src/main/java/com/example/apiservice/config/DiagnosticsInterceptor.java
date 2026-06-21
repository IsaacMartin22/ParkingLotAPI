package com.example.apiservice.config;

import com.example.apiservice.service.DiagnosticsService;
import com.example.apiservice.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DiagnosticsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "diagnosticsStartTime";

    private final MetricsService metricsService;

    public DiagnosticsInterceptor(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Object startTimeAttribute = request.getAttribute(START_TIME_ATTRIBUTE);
        long startTime = startTimeAttribute instanceof Long value ? value : System.currentTimeMillis();
        long durationMillis = System.currentTimeMillis() - startTime;

        String endpoint = getEndpointName(request);
        int statusCode = exception == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        metricsService.recordRequest(endpoint, statusCode, durationMillis);
    }

    private String getEndpointName(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (bestMatchingPattern != null) {
            return request.getMethod() + " " + bestMatchingPattern;
        }

        return request.getMethod() + " " + request.getRequestURI();
    }
}