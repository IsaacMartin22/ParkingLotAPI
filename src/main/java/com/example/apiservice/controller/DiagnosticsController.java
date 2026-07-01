package com.example.apiservice.controller;

import com.example.apiservice.pojo.ApiDiagnosticsResponse;
import com.example.apiservice.pojo.DatabaseDiagnosticsResponse;
import com.example.apiservice.service.ApiDiagnosticsService;
import com.example.apiservice.service.DatabaseDiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiagnosticsController {

    private final ApiDiagnosticsService apiDiagnosticsService;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;

    public DiagnosticsController(ApiDiagnosticsService apiDiagnosticsService, DatabaseDiagnosticsService databaseDiagnosticsService) {
        this.apiDiagnosticsService = apiDiagnosticsService;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
    }

    @GetMapping("/api/diagnostics/api")
    public ApiDiagnosticsResponse getApiDiagnostics() {
        return apiDiagnosticsService.getDiagnostics();
    }

    @GetMapping("/api/diagnostics/database")
    public DatabaseDiagnosticsResponse getDatabaseDiagnostics() {
        return databaseDiagnosticsService.getDiagnostics();
    }


}
