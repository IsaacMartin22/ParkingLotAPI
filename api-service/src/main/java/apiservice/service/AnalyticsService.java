package apiservice.service;

import apiservice.dbentity.Analytics;
import apiservice.mapper.AnalyticsMapper;
import apiservice.repository.AnalyticsRepository;
import parkinglot.common.model.AnalyticsEventTypes;
import parkinglot.common.request.AnalyticsRequest;
import org.springframework.stereotype.Service;
import parkinglot.common.response.AnalyticsResponse;

import java.util.List;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public List<Analytics> getAllAnalyticsEvents() {
        List<Analytics> analytics = analyticsRepository.getAll(0, 1000);

        return analytics;
    }

    public List<Analytics> getAnalyticsPage(int page) {
        List<Analytics> analytics = analyticsRepository.getAll(page * 1000, 1000);

        return analytics;
    }

    public List<Analytics> getAnalyticsEventsForType(AnalyticsEventTypes eventType) {
        List<Analytics> analytics = analyticsRepository.getByEventType(eventType, 0, 1000);

        return analytics;
    }

    public AnalyticsResponse recordAnalyticsEvent(AnalyticsRequest analyticsRequest) {
        validate(analyticsRequest);
        Analytics analytics = createEntity(analyticsRequest);
        analyticsRepository.save(analytics);
        return AnalyticsMapper.toResponse(analytics);
    }

    private void validate(AnalyticsRequest analyticsRequest) {
        if (analyticsRequest == null) {
            throw new IllegalArgumentException("Analytics Request required: " + analyticsRequest);
        }
        if (analyticsRequest.eventType() == null) {
            throw new IllegalArgumentException("Event Type required: " + analyticsRequest);
        }
        if (analyticsRequest.browser() == null) {
            throw new IllegalArgumentException("Browser required: " + analyticsRequest);
        }
        if (analyticsRequest.currentUrl() == null) {
            throw new IllegalArgumentException("CurrentUrl required: " + analyticsRequest);
        }
        if (analyticsRequest.ipAddress() == null) {
            throw new IllegalArgumentException("IP Address required: " + analyticsRequest);
        }
        if (analyticsRequest.payload() == null) {
            throw new IllegalArgumentException("Payload required: " + analyticsRequest);
        }
    }

    private Analytics createEntity(AnalyticsRequest analyticRequest) {
        Analytics newAnalytics = new Analytics();
        newAnalytics.setEventType(analyticRequest.eventType());
        newAnalytics.setCurrentUrl(analyticRequest.currentUrl());
        newAnalytics.setBrowser(analyticRequest.browser());
        newAnalytics.setOperatingSystem(analyticRequest.operatingSystem());
        newAnalytics.setIpAddress(analyticRequest.ipAddress());
        newAnalytics.setSessionId(analyticRequest.sessionId());
        newAnalytics.setTimestamp(analyticRequest.timestamp());
        newAnalytics.setPayload(analyticRequest.payload());
        return newAnalytics;
    }
}
