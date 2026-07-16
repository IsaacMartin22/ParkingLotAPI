package apiservice.controller;

import apiservice.dbentity.Analytics;
import apiservice.mapper.AnalyticsMapper;
import apiservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import parkinglot.common.model.AnalyticsEventTypes;
import parkinglot.common.request.AnalyticsQuery;
import parkinglot.common.request.AnalyticsRequest;
import parkinglot.common.response.AnalyticsResponse;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/search")
    public List<AnalyticsResponse> queryAnalytics(@RequestBody AnalyticsQuery analyticsQuery) {
        List<Analytics> analytics = analyticsService.queryAnalytics(analyticsQuery);
        return analytics.stream()
                .map(AnalyticsMapper::toResponse)
                .toList();
    }

    @PostMapping
    public AnalyticsResponse postAnalyticsClickEvent(@RequestBody AnalyticsRequest analyticsRequest) {
        return analyticsService.recordAnalyticsEvent(analyticsRequest);
    }
}
