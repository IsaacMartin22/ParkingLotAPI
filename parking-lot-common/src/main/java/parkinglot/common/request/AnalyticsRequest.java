package parkinglot.common.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import parkinglot.common.model.AnalyticsEventTypes;

import java.time.Instant;
import java.util.Map;

public record AnalyticsRequest (
    AnalyticsEventTypes eventType,
    String currentUrl,
    String browser,
    String operatingSystem,
    String ipAddress,
    @JsonProperty("session_id") String sessionId,
    Instant timestamp,
    Map<String, Object> payload
) {}
