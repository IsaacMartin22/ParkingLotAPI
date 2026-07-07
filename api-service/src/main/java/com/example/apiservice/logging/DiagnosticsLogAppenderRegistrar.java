package com.example.apiservice.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Attaches the {@link DiagnosticsLogAppender} to the logback root logger at
 * startup, filtered so only WARN and ERROR events are captured.
 */
@Component
public class DiagnosticsLogAppenderRegistrar {

    private final DiagnosticsLogStore logStore;

    public DiagnosticsLogAppenderRegistrar(DiagnosticsLogStore logStore) {
        this.logStore = logStore;
    }

    @PostConstruct
    public void register() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            // Logback isn't the active SLF4J backend; nothing to attach to.
            return;
        }

        ThresholdFilter warnFilter = new ThresholdFilter();
        warnFilter.setLevel(Level.WARN.levelStr);
        warnFilter.setContext(loggerContext);
        warnFilter.start();

        DiagnosticsLogAppender appender = new DiagnosticsLogAppender(logStore);
        appender.setName("diagnosticsLogAppender");
        appender.setContext(loggerContext);
        appender.addFilter(warnFilter);
        appender.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
    }
}
