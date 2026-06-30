package com.example.parkinglot.sdk;

import com.example.parkinglot.sdk.model.CarCreateRequest;
import com.example.parkinglot.sdk.model.CarResponse;
import com.example.parkinglot.sdk.model.CarUpdateRequest;
import com.example.parkinglot.sdk.model.DiagnosticsResponse;
import com.example.parkinglot.sdk.model.FloorDetailsResponse;
import com.example.parkinglot.sdk.model.FloorResponse;
import com.example.parkinglot.sdk.model.ParkingLotResponse;
import com.example.parkinglot.sdk.model.ParkingSpaceResponse;
import com.example.parkinglot.sdk.model.ParkingSpaceUpdateRequest;
import com.example.parkinglot.sdk.model.SectionResponse;
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
import java.util.Objects;

public class ParkingLotApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration requestTimeout;

    public ParkingLotApiClient(String baseUrl) {
        this(baseUrl, HttpClient.newHttpClient(), defaultObjectMapper(), Duration.ofSeconds(20));
    }

    public ParkingLotApiClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper, Duration requestTimeout) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout is required");
    }

    public List<CarResponse> getCars() {
        return getList("/api/cars", CarResponse.class);
    }

    public CarResponse getCar(long id) {
        return get("/api/cars/" + id, CarResponse.class);
    }

    public CarResponse createCar(CarCreateRequest request) {
        return post("/api/cars", request, CarResponse.class);
    }

    public CarResponse updateCar(long id, CarUpdateRequest request) {
        return put("/api/cars/" + id, request, CarResponse.class);
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

    private static String normalizeBaseUrl(String url) {
        String trimmed = Objects.requireNonNull(url, "baseUrl is required").trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

