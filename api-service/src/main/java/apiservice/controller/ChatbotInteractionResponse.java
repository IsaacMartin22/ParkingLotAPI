package apiservice.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChatbotInteractionResponse(
        String question,
        String response,
        Instant timestamp,
        @JsonProperty("cacheHit") Boolean cacheHit,
        @JsonProperty("embeddingLatencyMs") Long embeddingLatencyMs,
        @JsonProperty("vectorSearchDurationMs") Long vectorSearchDurationMs,
        @JsonProperty("vectorSearchDocumentCount") Integer vectorSearchDocumentCount,
        @JsonProperty("rating") Integer rating
) {
}
