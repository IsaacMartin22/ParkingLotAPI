package com.example.apiservice.controller;

import com.example.apiservice.service.ApiDiagnosticsService;
import com.example.apiservice.service.DatabaseDiagnosticsService;
import com.example.parkinglot.common.response.ApiDiagnosticsResponse;
import com.example.parkinglot.common.response.DatabaseDiagnosticsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final ApiDiagnosticsService apiDiagnosticsService;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;

    public DiagnosticsController(ApiDiagnosticsService apiDiagnosticsService, DatabaseDiagnosticsService databaseDiagnosticsService) {
        this.apiDiagnosticsService = apiDiagnosticsService;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
    }

    @GetMapping("/api")
    public ApiDiagnosticsResponse getApiDiagnostics() {
        return apiDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/database")
    public DatabaseDiagnosticsResponse getDatabaseDiagnostics() {
        return databaseDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }
}
