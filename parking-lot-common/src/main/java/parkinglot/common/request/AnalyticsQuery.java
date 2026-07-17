package parkinglot.common.request;

import java.util.List;

public record AnalyticsQuery(
    List<AnalyticsQueryFilter> filters,
    String sortField,
    String sortDirection,
    int page
) {}


//public record AnalyticsQueryFilter(
//        String field,
//        String operator,
//        String value
//) {}
// Keeping this commented so I can copy/paste this DTO to AI easier
