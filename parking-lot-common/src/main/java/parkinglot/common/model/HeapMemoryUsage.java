package parkinglot.common.model;

import java.lang.management.MemoryUsage;

public record HeapMemoryUsage(
        long used,
        long max
) {
    public static final long UNAVAILABLE_METRIC = -1L;

    public static HeapMemoryUsage from(MemoryUsage heapMemoryUsage) {
        if (heapMemoryUsage == null) {
            return unavailable();
        }

        return new HeapMemoryUsage(
                normalizeMetric(heapMemoryUsage.getUsed()),
                normalizeMetric(heapMemoryUsage.getMax())
        );
    }

    private static HeapMemoryUsage unavailable() {
        return new HeapMemoryUsage(UNAVAILABLE_METRIC, UNAVAILABLE_METRIC);
    }

    private static long normalizeMetric(long value) {
        return value >= 0 ? value : UNAVAILABLE_METRIC;
    }
}
