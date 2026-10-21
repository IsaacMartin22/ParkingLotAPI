package apiservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.context.ApplicationListener;

import java.lang.management.ManagementFactory;
import java.util.Comparator;

public class StartupDiagnostics implements ApplicationListener<SpringApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupDiagnostics.class);
    private final BufferingApplicationStartup startup;

    public StartupDiagnostics(BufferingApplicationStartup startup) {
        this.startup = startup;
    }

    @Override
    public void onApplicationEvent(SpringApplicationEvent event) {
        if (event instanceof ApplicationStartedEvent) {
            logTimeline("context_started");
        } else if (event instanceof ApplicationReadyEvent) {
            logTimeline("ready");
        } else if (event instanceof ApplicationFailedEvent failed) {
            logger.error("Startup failed. exceptionType={}", failed.getException().getClass().getSimpleName());
            logTimeline("failed");
        }
    }

    private void logTimeline(String phase) {
        Runtime runtime = Runtime.getRuntime();
        logger.info("Startup checkpoint. phase={}, jvmUptimeMs={}, processCpuTimeNs={}, processors={}, "
                        + "heapUsedBytes={}, heapMaxBytes={}",
                phase, ManagementFactory.getRuntimeMXBean().getUptime(),
                ProcessHandle.current().info().totalCpuDuration().map(duration -> duration.toNanos()).orElse(-1L),
                runtime.availableProcessors(), runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory());
        // Steps nest: durations include child steps and must not be added together.
        startup.getBufferedTimeline().getEvents().stream()
                .filter(step -> step.getDuration().toMillis() >= 250)
                .sorted(Comparator.comparing(StartupTimeline.TimelineEvent::getDuration).reversed())
                .limit(20)
                .forEach(step -> {
                    String beanName = "";
                    for (var tag : step.getStartupStep().getTags()) {
                        if ("beanName".equals(tag.getKey())) {
                            beanName = tag.getValue();
                        }
                    }
                    logger.info("Slow startup step (inclusive). phase={}, step={}, stepId={}, parentId={}, "
                                    + "beanName={}, durationMs={}",
                            phase, step.getStartupStep().getName(), step.getStartupStep().getId(),
                            step.getStartupStep().getParentId(), beanName, step.getDuration().toMillis());
                });
    }
}
