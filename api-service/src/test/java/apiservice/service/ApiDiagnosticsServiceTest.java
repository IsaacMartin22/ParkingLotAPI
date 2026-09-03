package apiservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import parkinglot.common.model.HeapMemoryUsage;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiDiagnosticsServiceTest {

    @Test
    void includesHeapMemoryMetricsInApiDiagnostics() {
        ObjectProvider<RequestMappingHandlerMapping> mappingProvider = mock(ObjectProvider.class);
        MemoryMXBean memoryMXBean = mock(MemoryMXBean.class);
        when(memoryMXBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 128L, 256L, 512L));

        var response = new ApiDiagnosticsService(mappingProvider, memoryMXBean).getDiagnostics();

        assertEquals(128L, response.heapMemoryUsage().used());
        assertEquals(512L, response.heapMemoryUsage().max());
    }

    @Test
    void marksUndefinedHeapMaximumAsUnavailable() {
        HeapMemoryUsage usage = HeapMemoryUsage.from(new MemoryUsage(0, 128L, 256L, -1L));

        assertEquals(128L, usage.used());
        assertEquals(HeapMemoryUsage.UNAVAILABLE_METRIC, usage.max());
    }
}
