package parkinglot.common.request;

public record AnalyticsQueryFilter(
    String field,
    String operator,
    String value
) {}
