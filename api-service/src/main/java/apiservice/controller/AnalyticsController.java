package apiservice.controller;

import apiservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import parkinglot.common.model.AnalyticsEventTypes;
import parkinglot.common.request.AnalyticsQuery;
import parkinglot.common.request.AnalyticsRequest;
import parkinglot.common.response.AnalyticsQueryResponse;
import parkinglot.common.response.AnalyticsResponse;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/search")
    public AnalyticsQueryResponse queryAnalytics(@RequestBody AnalyticsQuery analyticsQuery) {
        return analyticsService.queryAnalytics(analyticsQuery);
    }

    @PostMapping
    public AnalyticsResponse postAnalyticsClickEvent(@RequestBody AnalyticsRequest analyticsRequest) {
        return analyticsService.recordAnalyticsEvent(analyticsRequest);
    }
}
