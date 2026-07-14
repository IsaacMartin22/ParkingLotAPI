package apiservice.controller;

import apiservice.service.IntegrationsService;
import org.springframework.web.bind.annotation.*;
import parkinglot.common.response.BuildkiteBuildResponse;
import parkinglot.common.response.DeploymentResponse;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationsController {
    private final IntegrationsService integrationsService;

    public IntegrationsController(IntegrationsService integrationsService) {
        this.integrationsService = integrationsService;
    }

    @GetMapping("/bk")
    public List<BuildkiteBuildResponse> getBuildkiteInfo() {
        List<BuildkiteBuildResponse> buildkiteResponse = integrationsService.getBuildkiteInfo();
        return buildkiteResponse;
    }

    @GetMapping("/deploy")
    public List<DeploymentResponse> getDeployInfo() {
        List<DeploymentResponse> deploymentResponse = integrationsService.getDeployInfo();
        return deploymentResponse;
    }
}
