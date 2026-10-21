package apiservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void logsCheckpointWithoutExposingStartupTagValues(CapturedOutput output) throws InterruptedException {
        var startup = new BufferingApplicationStartup(16);
        var step = startup.start("test.startup").tag("beanName", "testBean")
                .tag("private", "private-startup-value");
        Thread.sleep(300);
        step.end();
        var diagnostics = new StartupDiagnostics(startup);
        try (var context = new GenericApplicationContext()) {
            diagnostics.onApplicationEvent(new ApplicationStartedEvent(
                    new SpringApplication(), new String[0], context, Duration.ZERO));
        }

        assertThat(output.getAll()).contains("phase=context_started", "jvmUptimeMs=", "heapMaxBytes=",
                        "step=test.startup", "beanName=testBean", "durationMs=")
                .doesNotContain("private-startup-value");
        assertThat(startup.getBufferedTimeline().getEvents()).hasSize(1);
    }
}
