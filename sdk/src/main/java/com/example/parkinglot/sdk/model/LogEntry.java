package com.example.parkinglot.sdk.model;

import lombok.Data;

import java.time.Instant;

@Data
public class LogEntry {
    private Instant timestamp;
    private String level;
    private String logger;
    private String thread;
    private String message;
    private String throwable;
}
