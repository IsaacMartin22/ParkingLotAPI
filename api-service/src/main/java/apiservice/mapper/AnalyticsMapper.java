package apiservice.mapper;

import apiservice.dbentity.Analytics;
import parkinglot.common.response.AnalyticsResponse;

public class AnalyticsMapper {
    public static AnalyticsResponse toResponse(Analytics analytics) {
        if (analytics == null) {
            return null;
        }

        return new AnalyticsResponse(
            analytics.getEventType(),
            analytics.getCurrentUrl(),
            analytics.getBrowser(),
            analytics.getOperatingSystem(),
            analytics.getIpAddress(),
            analytics.getSessionId(),
            analytics.getTimestamp(),
            analytics.getPayload()
        );
    }
}
