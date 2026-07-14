package apiservice.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import parkinglot.common.response.BuildkiteBuildResponse;
import parkinglot.common.response.DeploymentResponse;

import java.util.Arrays;
import java.util.List;

@Service
public class IntegrationsService {
    private final RestTemplate restTemplate;
    private final String buildkiteUrl = "https://api.buildkite.com/v2/builds?branch=main";
    private final String renderUrl = "https://api.render.com/v1/services/srv-d8tefhurnols73e9keu0/deploys";

    private final String buildkiteToken;
    private final String renderToken;

    public IntegrationsService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${BUILDKITE_API_KEY:}") String buildkiteToken,
            @Value("${RENDER_API_KEY:}") String renderToken
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.buildkiteToken = buildkiteToken;
        this.renderToken = renderToken;
    }

    public List<BuildkiteBuildResponse> getBuildkiteInfo() {
        return sendGetList(buildkiteUrl, buildkiteToken, BuildkiteBuildResponse[].class);
    }

    public List<DeploymentResponse> getDeployInfo() {
        return sendGetList(renderUrl, renderToken, DeploymentResponse[].class);
    }

    private <T> List<T> sendGetList(String url, String token, Class<T[]> responseArrayType) {
        validateConfig(url, token);

        HttpEntity<Void> request = new HttpEntity<>(createBearerHeaders(token));
        ResponseEntity<T[]> response = restTemplate.exchange(url, HttpMethod.GET, request, responseArrayType);
        T[] responseBody = response.getBody();
        if (responseBody == null) {
            return List.of();
        }
        return Arrays.asList(responseBody);
    }

    private HttpHeaders createBearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void validateConfig(String url, String token) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("Integration URL must be configured.");
        }
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Integration API token must be configured.");
        }
    }
}
