package com.example.apiservice.controller;

import com.example.apiservice.pojo.DiagnosticsResponse;
import com.example.apiservice.service.DiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiagnosticsController {

    private final DiagnosticsService diagnosticsService;

    public DiagnosticsController(DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/api/diagnostics")
    public DiagnosticsResponse getDiagnostics() {
        return diagnosticsService.getDiagnostics();
    }
}
