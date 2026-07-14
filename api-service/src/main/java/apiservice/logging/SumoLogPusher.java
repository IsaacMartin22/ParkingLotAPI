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

        encoder.start();

        if (endpointUrl == null || endpointUrl.isBlank()) {
            addInfo("No Sumo HTTP source URL configured; log shipping is disabled");
        }

        super.start();
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
            if (statusCode < 200 || statusCode >= 300) {
                addError("Failed to send log event to Sumo; HTTP status " + statusCode);
            }
        } catch (IOException | IllegalArgumentException ex) {
            addError("Failed to send log event to Sumo", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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
