package apiservice;

import apiservice.config.StartupDiagnostics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.management.ManagementFactory;
import java.time.Instant;

@SpringBootApplication
@EnableScheduling
public class ApiServiceApplication {

    public static void main(String[] args) {
        // Log before Spring initializes logging, including time spent before main (for example, Java agents).
        System.err.printf("[startup] service=api phase=main timestamp=%s jvmUptimeMs=%d%n",
                Instant.now(), ManagementFactory.getRuntimeMXBean().getUptime());
        var startup = new BufferingApplicationStartup(4096);
        var application = new SpringApplication(ApiServiceApplication.class);
        application.setApplicationStartup(startup);
        application.addListeners(new StartupDiagnostics(startup));
        application.run(args);
    }
}
