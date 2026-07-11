package parkinglot.common.model;

public record LongRunningQuery(
        long timeRunningMillis,
        String queryText
) {
}


