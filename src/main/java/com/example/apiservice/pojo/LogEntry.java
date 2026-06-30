package com.example.apiservice.pojo;

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
