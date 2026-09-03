package apiservice.controller;

import apiservice.service.ApiDiagnosticsService;
import apiservice.service.DatabaseDiagnosticsService;
import apiservice.service.MongoDatabaseDiagnosticsService;
import parkinglot.common.response.ApiDiagnosticsResponse;
import parkinglot.common.response.DatabaseDiagnosticsResponse;
import parkinglot.common.response.MongoDatabaseDiagnosticsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final ApiDiagnosticsService apiDiagnosticsService;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;
    private final MongoDatabaseDiagnosticsService mongoDatabaseDiagnosticsService;

    public DiagnosticsController(
            ApiDiagnosticsService apiDiagnosticsService,
            DatabaseDiagnosticsService databaseDiagnosticsService,
            MongoDatabaseDiagnosticsService mongoDatabaseDiagnosticsService
    ) {
        this.apiDiagnosticsService = apiDiagnosticsService;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
        this.mongoDatabaseDiagnosticsService = mongoDatabaseDiagnosticsService;
    }

    @GetMapping("/api")
    public ApiDiagnosticsResponse getApiDiagnostics() {
        return apiDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/database")
    public DatabaseDiagnosticsResponse getDatabaseDiagnostics() {
        return databaseDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/mongodb")
    public MongoDatabaseDiagnosticsResponse getMongoDatabaseDiagnostics() {
        return mongoDatabaseDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }
}
