package apiservice.logging;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class SumoLogPusher extends AppenderBase<ILoggingEvent> {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MILLIS = 250L;

    private Encoder<ILoggingEvent> encoder;
    private String endpointUrl;
    private int connectTimeoutMillis = 2000;
    private int readTimeoutMillis = 2000;

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }
    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder configured for SumoLogPusher");
            return;
        }

        //encoder.start();

        if (endpointUrl == null || endpointUrl.isBlank()) {
            addInfo("No Sumo HTTP source URL configured; log shipping is disabled");
        }

        //super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return;
        }

        byte[] payload = encoder.encode(eventObject);
        if (payload == null || payload.length == 0) {
            return;
        }

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(endpointUrl).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(connectTimeoutMillis);
                connection.setReadTimeout(readTimeoutMillis);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }

                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return;
                }

                if (statusCode == 429 || statusCode >= 500) {
                    if (attempt < MAX_RETRIES) {
                        long backoffMs = BASE_BACKOFF_MILLIS * (1L << attempt);
                        addWarn("Failed to send log event to Sumo; HTTP status " + statusCode + ". Retrying in " + backoffMs + " ms (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ")");
                        sleep(backoffMs);
                        continue;
                    }
                    addError("Failed to send log event to Sumo after retries; HTTP status " + statusCode);
                    return;
                }

                addError("Failed to send log event to Sumo; HTTP status " + statusCode);
                return;
            } catch (IOException | IllegalArgumentException ex) {
                if (attempt < MAX_RETRIES) {
                    long backoffMs = BASE_BACKOFF_MILLIS * (1L << attempt);
                    addWarn("Failed to send log event to Sumo; retrying in " + backoffMs + " ms (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ")", ex);
                    sleep(backoffMs);
                    continue;
                }
                addError("Failed to send log event to Sumo", ex);
                return;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private void sleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            addWarn("Interrupted while backing off Sumo log delivery", ex);
        }
    }

    @Override
    public void stop() {
        if (encoder != null) {
            encoder.stop();
        }
        super.stop();
    }
}
