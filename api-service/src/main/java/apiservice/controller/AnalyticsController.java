package apiservice.controller;

import apiservice.dbentity.Analytics;
import apiservice.mapper.AnalyticsMapper;
import apiservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import parkinglot.common.model.AnalyticsEventTypes;
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

    @GetMapping(params = "!page")
    public List<AnalyticsResponse> getAllAnalyticsEvents() {
        List<Analytics> analytics = analyticsService.getAllAnalyticsEvents();
        return analytics.stream()
                .map(AnalyticsMapper::toResponse)
                .toList();
    }

    @GetMapping(params = {"page", "sort"})
    public List<AnalyticsResponse> getAnalyticsPage(@RequestParam int page, @RequestParam String sort) {
        List<Analytics> analytics = analyticsService.getAnalyticsPage(page, sort);
        return analytics.stream()
                .map(AnalyticsMapper::toResponse)
                .toList();
    }

    @GetMapping("/{eventType}")
    public List<AnalyticsResponse> getAnalyticsEventsForType(@PathVariable AnalyticsEventTypes eventType) {
        List<Analytics> analytics = analyticsService.getAnalyticsEventsForType(eventType);
        return analytics.stream()
                .map(AnalyticsMapper::toResponse)
                .toList();
    }

    @PostMapping
    public AnalyticsResponse postAnalyticsClickEvent(@RequestBody AnalyticsRequest analyticsRequest) {
        return analyticsService.recordAnalyticsEvent(analyticsRequest);
    }
}
