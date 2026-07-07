package com.example.apiservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String endpoint, int statusCode, long durationMillis) {
        Counter.builder("api.requests.total")
                .description("Total number of API requests")
                .tag("endpoint", endpoint)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();

        Timer.builder("api.requests.duration")
                .description("API request duration")
                .tag("endpoint", endpoint)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMillis));
    }
}
