package apiservice.controller;

import java.time.Instant;

public record ChatbotInteractionResponse(
        String question,
        String response,
        Instant timestamp
) {
}
