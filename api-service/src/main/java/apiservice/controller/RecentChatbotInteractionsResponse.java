package apiservice.controller;

import java.util.List;

public record RecentChatbotInteractionsResponse(
        List<ChatbotInteractionResponse> interactions
) {
}
