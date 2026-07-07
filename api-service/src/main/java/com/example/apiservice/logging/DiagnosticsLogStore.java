package com.example.apiservice.logging;

import com.example.parkinglot.common.model.LogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * In-memory, bounded ring buffer of the most recent WARN/ERROR log entries
 * captured since the service started. Once capacity is reached the oldest
 * entry is evicted so the store only ever retains the most recent logs.
 */
@Component
public class DiagnosticsLogStore {

    /** Maximum number of warn/error entries retained at any time. */
    public static final int MAX_ENTRIES = 1000;

    private final int capacity;
    private final Deque<LogEntry> entries = new ArrayDeque<>();

    public DiagnosticsLogStore() {
        this(MAX_ENTRIES);
    }

    DiagnosticsLogStore(int capacity) {
        this.capacity = capacity;
    }

    public void add(LogEntry entry) {
        synchronized (entries) {
            if (entries.size() >= capacity) {
                entries.removeFirst();
            }
            entries.addLast(entry);
        }
    }

    /**
     * Returns a snapshot of the retained entries, most recent first.
     */
    public List<LogEntry> snapshot() {
        List<LogEntry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries);
        }
        Collections.reverse(snapshot);
        return snapshot;
    }
}
