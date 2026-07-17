package apiservice.repository;

import apiservice.dbentity.Analytics;

import java.util.List;

public record AnalyticsQueryResult(
        List<Analytics> results,
        long totalCount
) {}
