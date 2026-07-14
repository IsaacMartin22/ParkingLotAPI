package parkinglot.common.response;

import parkinglot.common.model.AnalyticsEventTypes;

import java.time.Instant;
import java.util.Map;

public record AnalyticsResponse(
        AnalyticsEventTypes eventType,
        String currentUrl,
        String browser,
        String operatingSystem,
        String ipAddress,
        String sessionId,
        Instant timestamp,
        Map<String, Object> extraFields
) {
}
