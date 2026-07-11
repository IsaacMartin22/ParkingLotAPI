package parkinglot.common.request;

import parkinglot.common.model.AnalyticsEventTypes;

import java.time.Instant;
import java.util.Map;

public record AnalyticsRequest (
    AnalyticsEventTypes eventType,
    String currentUrl,
    String browser,
    String operatingSystem,
    String ipAddress,
    Instant timestamp,
    Map<String, Object> payload
) {}
