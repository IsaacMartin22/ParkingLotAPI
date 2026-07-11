package parkinglot.common.model;

import java.time.Instant;

public record LogEntry(
        Instant timestamp,
        String level,
        String logger,
        String thread,
        String message,
        String throwable
) {
}

