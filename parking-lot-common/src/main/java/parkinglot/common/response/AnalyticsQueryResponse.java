package parkinglot.common.response;

import java.util.List;

public record AnalyticsQueryResponse(
        List<AnalyticsResponse> results,
        long totalCount,
        int totalPages,
        int pageSize
) {}
