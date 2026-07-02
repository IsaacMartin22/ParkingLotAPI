package com.example.parkinglot.sdk;

// Car endpoints have been removed; related model types are no longer imported
import com.example.parkinglot.sdk.model.responses.DiagnosticsResponse;
import com.example.parkinglot.sdk.model.responses.FloorDetailsResponse;
import com.example.parkinglot.sdk.model.responses.FloorResponse;
import com.example.parkinglot.sdk.model.responses.ParkingLotResponse;
import com.example.parkinglot.sdk.model.responses.ParkingSpaceResponse;
import com.example.parkinglot.sdk.model.requests.ParkingSpaceUpdateRequest;
import com.example.parkinglot.sdk.model.responses.SectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class ParkingLotApiClient {
    private static final String DEFAULT_BASE_URL = "https://api-service-i1ms.onrender.com/api";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration requestTimeout;

    public ParkingLotApiClient() {
        this.baseUrl = DEFAULT_BASE_URL;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = defaultObjectMapper();
        this.requestTimeout = Duration.ofSeconds(30);
    }


    public List<ParkingSpaceResponse> getParkingSpaces() {
        return getList("/api/spaces", ParkingSpaceResponse.class);
    }

    public ParkingSpaceResponse getParkingSpace(long id) {
        return get("/api/spaces/" + id, ParkingSpaceResponse.class);
    }

    public ParkingSpaceResponse updateParkingSpace(long id, ParkingSpaceUpdateRequest request) {
        return put("/api/spaces/" + id, request, ParkingSpaceResponse.class);
    }

    public List<FloorResponse> getFloors() {
        return getList("/api/floors", FloorResponse.class);
    }

    public FloorResponse getFloor(long id) {
        return get("/api/floors/" + id, FloorResponse.class);
    }

    public List<SectionResponse> getSections() {
        return getList("/api/sections", SectionResponse.class);
    }

    public SectionResponse getSection(long id) {
        return get("/api/sections/" + id, SectionResponse.class);
    }

    public List<ParkingLotResponse> getParkingLots() {
        return getList("/api/lots", ParkingLotResponse.class);
    }

    public ParkingLotResponse getParkingLot(long id) {
        return get("/api/lots/" + id, ParkingLotResponse.class);
    }

    public FloorDetailsResponse getFloorDetailsForLot(long lotId, long floorId) {
        return get("/api/lots/" + lotId + "/floors/" + floorId + "/details", FloorDetailsResponse.class);
    }

    public DiagnosticsResponse getDiagnostics() {
        return get("/api/diagnostics", DiagnosticsResponse.class);
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = requestBuilder(path).GET().build();
        return send(request, responseType);
    }

    private <T> List<T> getList(String path, Class<T> elementType) {
        HttpRequest request = requestBuilder(path).GET().build();
        String json = sendForString(request);
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (JsonProcessingException ex) {
            throw new ApiClientException("Failed to deserialize response list from " + path, ex);
        }
    }

    private <B, T> T post(String path, B body, Class<T> responseType) {
        HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(serialize(body)))
                .build();
        return send(request, responseType);
    }

    private <B, T> T put(String path, B body, Class<T> responseType) {
        HttpRequest request = requestBuilder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(serialize(body)))
                .build();
        return send(request, responseType);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
    }

    private String serialize(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new ApiClientException("Failed to serialize request body", ex);
        }
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        String json = sendForString(request);
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException ex) {
            throw new ApiClientException("Failed to deserialize response", ex);
        }
    }

    private String sendForString(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiClientException("HTTP call interrupted", ex);
        } catch (IOException ex) {
            throw new ApiClientException("HTTP call failed", ex);
        }

        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new ApiClientHttpException(code, response.body());
        }

        return response.body();
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}

