package apiservice.controller;

import apiservice.service.ApiDiagnosticsService;
import apiservice.service.DatabaseDiagnosticsService;
import apiservice.service.MongoDatabaseDiagnosticsService;
import org.junit.jupiter.api.Test;
import parkinglot.common.response.MongoDatabaseDiagnosticsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiagnosticsControllerTest {

    @Test
    void getMongoDatabaseDiagnosticsDelegatesToMongoDiagnosticsService() {
        ApiDiagnosticsService apiDiagnosticsService = mock(ApiDiagnosticsService.class);
        DatabaseDiagnosticsService databaseDiagnosticsService = mock(DatabaseDiagnosticsService.class);
        MongoDatabaseDiagnosticsService mongoDatabaseDiagnosticsService = mock(MongoDatabaseDiagnosticsService.class);
        MongoDatabaseDiagnosticsResponse expected = new MongoDatabaseDiagnosticsResponse(
                true, 2L, 120_000L, 4L, 100L, 4_096L, List.of()
        );
        DiagnosticsController controller = new DiagnosticsController(
                apiDiagnosticsService,
                databaseDiagnosticsService,
                mongoDatabaseDiagnosticsService
        );
        when(mongoDatabaseDiagnosticsService.getDiagnostics()).thenReturn(expected);

        var response = controller.getMongoDatabaseDiagnostics();

        assertSame(expected, response);
        verify(mongoDatabaseDiagnosticsService).getDiagnostics();
    }
}
