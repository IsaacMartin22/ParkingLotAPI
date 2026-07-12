package apiservice.interceptor;

import apiservice.service.ApiDiagnosticsService;
import apiservice.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestDiagnosticsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestDiagnosticsInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = RequestDiagnosticsInterceptor.class.getName() + ".startTime";

    private final ApiDiagnosticsService apiDiagnosticsService;
    private final MetricsService metricsService;

    public RequestDiagnosticsInterceptor(ApiDiagnosticsService apiDiagnosticsService, MetricsService metricsService) {
        this.apiDiagnosticsService = apiDiagnosticsService;
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
            Exception ex
    ) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        long startMillis = startTime instanceof Long ? (Long) startTime : System.currentTimeMillis();
        long durationMillis = Math.max(0L, System.currentTimeMillis() - startMillis);
        String endpoint = resolveEndpoint(request);
        int status = response.getStatus();
        boolean successful = ex == null && status < 400;

        if (successful) {
            apiDiagnosticsService.recordSuccess(endpoint, durationMillis);
        } else {
            apiDiagnosticsService.recordFailure(endpoint, durationMillis);
        }

        metricsService.recordRequest(endpoint, status, durationMillis);
        log.info(
                "HTTP request completed method={} path={} uri={} status={} durationMs={} success={}",
                request.getMethod(),
                getPath(request),
                request.getRequestURI(),
                status,
                durationMillis,
                successful
        );
    }

    private String resolveEndpoint(HttpServletRequest request) {
        return request.getMethod() + " " + getPath(request);
    }

    private String getPath(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String ? (String) pattern : request.getRequestURI();
    }
}
