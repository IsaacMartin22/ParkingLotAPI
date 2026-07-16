package parkinglot.common.request;

import java.util.List;

public record AnalyticsQuery(
    List<AnalyticsQueryFilter> filters,
    String sortField,
    String sortDirection,
    int page
) {}
