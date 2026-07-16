package apiservice.service;

import apiservice.dbentity.Analytics;
import apiservice.mapper.AnalyticsMapper;
import apiservice.repository.AnalyticsRepository;
import parkinglot.common.request.AnalyticsQuery;
import parkinglot.common.request.AnalyticsQueryFilter;
import parkinglot.common.request.AnalyticsRequest;
import org.springframework.stereotype.Service;
import parkinglot.common.response.AnalyticsResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsService {
    private static final int PAGE_SIZE = 1000;

    private static final Map<String, Set<String>> ALLOWED_FIELDS_AND_FILTER_OPERATORS = Map.of(
            "eventType", Set.of("eq", "neq"),
            "currentUrl", Set.of("eq", "neq", "has"),
            "browser", Set.of("eq", "neq", "has"),
            "operatingSystem", Set.of("eq", "neq", "has"),
            "sessionId", Set.of("eq", "neq", "has"),
            "ipAddress", Set.of("eq", "neq", "has"),
            "timestamp", Set.of("eq", "neq", "lt", "lte", "gt", "gte")
    );

    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of(
            "asc",
            "desc"
    );

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public List<Analytics> queryAnalytics(AnalyticsQuery analyticsQuery) {
        validateAnalyticsQuery(analyticsQuery);
        return analyticsRepository.query(
                PAGE_SIZE * (analyticsQuery.page() - 1),
                PAGE_SIZE,
                analyticsQuery.sortField(),
                analyticsQuery.sortDirection(),
                analyticsQuery.filters()
        );
    }

    public AnalyticsResponse recordAnalyticsEvent(AnalyticsRequest analyticsRequest) {
        Analytics analytics = validateAndCreateEntity(analyticsRequest);
        Analytics savedAnalytics = analyticsRepository.save(analytics);
        return AnalyticsMapper.toResponse(savedAnalytics);
    }

    private void validate(AnalyticsRequest analyticsRequest) {
        if (analyticsRequest == null) {
            throw new IllegalArgumentException("Analytics Request required");
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

    private Analytics validateAndCreateEntity(AnalyticsRequest analyticRequest) {
        validate(analyticRequest);

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

    private void validateAnalyticsQuery(AnalyticsQuery analyticsQuery) {
        if (analyticsQuery.page() < 1 || analyticsQuery.page() > 1_000) {
            throw new IllegalArgumentException("Page must be between 1 and 1_000_000");
        }

        if (!ALLOWED_FIELDS_AND_FILTER_OPERATORS.containsKey(analyticsQuery.sortField())) {
            throw new IllegalArgumentException("Invalid sort field: " + analyticsQuery.sortField());
        }

        if (!ALLOWED_SORT_DIRECTIONS.contains(analyticsQuery.sortDirection())) {
            throw new IllegalArgumentException("Invalid sort value: " + analyticsQuery.sortDirection());
        }

        if (analyticsQuery.filters() == null) {
            throw new IllegalArgumentException("Filters required");
        }

        for (AnalyticsQueryFilter filter : analyticsQuery.filters()) {
            if (!ALLOWED_FIELDS_AND_FILTER_OPERATORS.containsKey(filter.field())) {
                throw new IllegalArgumentException("Invalid filter field: " + filter);
            }

            Set<String> allowedOperators = ALLOWED_FIELDS_AND_FILTER_OPERATORS.get(filter.field());
            if (allowedOperators == null || !allowedOperators.contains(filter.operator())) {
                throw new IllegalArgumentException("Invalid filter operator for field " + filter.field() + ": " + filter.operator());
            }
        }
    }
}
