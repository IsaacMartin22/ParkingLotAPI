package apiservice.config;

import apiservice.logging.DiagnosticsLogStore;
import parkinglot.common.model.LogEntry;
import apiservice.service.ApiDiagnosticsService;
import apiservice.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@Slf4j
@Component
public class DiagnosticsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "diagnosticsStartTime";

    private final MetricsService metricsService;
    private final ApiDiagnosticsService apiDiagnosticsService;
    private final DiagnosticsLogStore diagnosticsLogStore;

    public DiagnosticsInterceptor(
            MetricsService metricsService,
            ApiDiagnosticsService apiDiagnosticsService,
            DiagnosticsLogStore diagnosticsLogStore
    ) {
        this.metricsService = metricsService;
        this.apiDiagnosticsService = apiDiagnosticsService;
        this.diagnosticsLogStore = diagnosticsLogStore;
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
        apiDiagnosticsService.recordRequest(endpoint, statusCode, durationMillis);
        if (statusCode >= 400) {
            String message = String.format(
                    "Request failed: %s %s - Status: %d, Duration: %d ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    statusCode,
                    durationMillis
            );
            diagnosticsLogStore.add(new LogEntry(
                    Instant.now(),
                    "WARN",
                    DiagnosticsInterceptor.class.getName(),
                    Thread.currentThread().getName(),
                    message,
                    exception == null ? null : getStackTrace(exception)
            ));
            log.warn(message, exception);
        }
        else {
            log.info("Request completed: {} {} - Status: {}, Duration: {} ms", request.getMethod(), request.getRequestURI(), statusCode, durationMillis);
        }
    }

    private static String getStackTrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String getEndpointName(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (bestMatchingPattern != null) {
            return request.getMethod() + " " + bestMatchingPattern;
        }

        return request.getMethod() + " " + request.getRequestURI();
    }
}