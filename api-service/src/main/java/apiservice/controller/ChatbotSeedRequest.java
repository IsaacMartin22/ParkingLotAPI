package apiservice.controller;

import jakarta.validation.constraints.NotBlank;

public record ChatbotSeedRequest(
        @NotBlank(message = "Text is required") String text,
        @NotBlank(message = "Category is required") String category,
        @NotBlank(message = "Source is required") String source
) {
}
