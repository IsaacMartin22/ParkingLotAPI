package com.example.apiservice.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.example.parkinglot.common.model.LogEntry;

import java.time.Instant;

/**
 * Logback appender that forwards WARN and ERROR events into the
 * {@link DiagnosticsLogStore} so they can be surfaced by the diagnostics endpoint.
 */
public class DiagnosticsLogAppender extends AppenderBase<ILoggingEvent> {

    private final DiagnosticsLogStore logStore;

    public DiagnosticsLogAppender(DiagnosticsLogStore logStore) {
        this.logStore = logStore;
    }

    @Override
    protected void append(ILoggingEvent event) {
        logStore.add(new LogEntry(
                Instant.ofEpochMilli(event.getTimeStamp()),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                formatThrowable(event.getThrowableProxy())
        ));
    }

    private static String formatThrowable(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        return ThrowableProxyUtil.asString(throwableProxy);
    }
}
