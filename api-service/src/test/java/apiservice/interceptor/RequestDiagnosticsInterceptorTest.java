package apiservice.interceptor;

import apiservice.service.ApiDiagnosticsService;
import apiservice.service.MetricsService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RequestDiagnosticsInterceptorTest {

    @Mock
    private ApiDiagnosticsService apiDiagnosticsService;

    @Mock
    private MetricsService metricsService;

    private RequestDiagnosticsInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RequestDiagnosticsInterceptor(apiDiagnosticsService, metricsService);
    }

    @Test
    void recordsSuccessForSuccessfulResponse() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestDiagnosticsInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/analytics");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/analytics");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(HttpServletResponse.SC_OK);

            interceptor.preHandle(request, response, new Object());
            interceptor.afterCompletion(request, response, new Object(), null);

            verify(apiDiagnosticsService).recordSuccess(eq("GET /api/analytics"), anyLong());
            verify(metricsService).recordRequest(eq("GET /api/analytics"), eq(HttpServletResponse.SC_OK), anyLong());
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("HTTP request completed");
                assertThat(event.getFormattedMessage()).contains("method=GET");
                assertThat(event.getFormattedMessage()).contains("path=/api/analytics");
                assertThat(event.getFormattedMessage()).contains("status=200");
            });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void recordsFailureForClientErrorResponse() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestDiagnosticsInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lots/1");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/lots/{id}");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            interceptor.preHandle(request, response, new Object());
            interceptor.afterCompletion(request, response, new Object(), null);

            verify(apiDiagnosticsService).recordFailure(eq("GET /api/lots/{id}"), anyLong());
            verify(metricsService).recordRequest(eq("GET /api/lots/{id}"), eq(HttpServletResponse.SC_NOT_FOUND), anyLong());
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("HTTP request completed");
                assertThat(event.getFormattedMessage()).contains("method=GET");
                assertThat(event.getFormattedMessage()).contains("path=/api/lots/{id}");
                assertThat(event.getFormattedMessage()).contains("status=404");
            });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
