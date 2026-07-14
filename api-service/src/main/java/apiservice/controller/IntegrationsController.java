package apiservice.controller;

import apiservice.service.IntegrationsService;
import org.springframework.web.bind.annotation.*;
import parkinglot.common.request.TrelloFeatureRequest;
import parkinglot.common.response.BuildkiteBuildResponse;
import parkinglot.common.response.DeploymentResponse;
import parkinglot.common.response.TrelloResponse;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationsController {
    private final IntegrationsService integrationsService;

    public IntegrationsController(IntegrationsService integrationsService) {
        this.integrationsService = integrationsService;
    }

    @GetMapping("/trello")
    public List<TrelloResponse> getTrelloInfo() {
        List<TrelloResponse> trelloResponse = integrationsService.getTrelloBoard();
        return trelloResponse;
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

    @PostMapping("/trello")
    public TrelloResponse postTrelloFeatureRequest(@RequestBody TrelloFeatureRequest trelloFeatureRequest) {
        return integrationsService.postTrelloFeatureRequest(trelloFeatureRequest);
    }
}
