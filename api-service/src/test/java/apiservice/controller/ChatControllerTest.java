package apiservice.controller;

import apiservice.dbentity.ChatInteraction;
import apiservice.repository.ChatInteractionRepository;
import apiservice.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    @Test
    void recentInteractionsIncludePersistedCompletionMetrics() {
        ChatService chatService = mock(ChatService.class);
        ChatInteractionRepository repository = mock(ChatInteractionRepository.class);
        ChatInteraction interaction = new ChatInteraction();
        Instant timestamp = Instant.parse("2026-09-06T18:00:00Z");
        interaction.setQuestion("What Java experience does Isaac have?");
        interaction.setAnswer("He has backend Java experience.");
        interaction.setCreatedAt(timestamp);
        interaction.setCacheHit(false);
        interaction.setEmbeddingLatencyMs(42L);
        interaction.setVectorSearchDurationMs(17L);
        interaction.setVectorSearchDocumentCount(3);
        interaction.setRating(5);
        when(repository.findByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(interaction));

        ChatController controller = new ChatController(chatService, repository);

        ChatbotInteractionResponse response = controller.getRecentChatbotInteractions()
                .getBody()
                .interactions()
                .get(0);

        assertEquals("What Java experience does Isaac have?", response.question());
        assertEquals("He has backend Java experience.", response.response());
        assertEquals(timestamp, response.timestamp());
        assertEquals(false, response.cacheHit());
        assertEquals(42L, response.embeddingLatencyMs());
        assertEquals(17L, response.vectorSearchDurationMs());
        assertEquals(3, response.vectorSearchDocumentCount());
        assertEquals(5, response.rating());
    }

    @Test
    void interactionMetricsKeepTheirCamelCaseNamesWhenValuesAreUnknown() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new ChatbotInteractionResponse(
                "Question",
                "Answer",
                null,
                null,
                null,
                null,
                null,
                null
        ));

        var response = new ObjectMapper().readTree(json);

        assertEquals(true, response.has("cacheHit"));
        assertEquals(true, response.has("embeddingLatencyMs"));
        assertEquals(true, response.has("vectorSearchDurationMs"));
        assertEquals(true, response.has("vectorSearchDocumentCount"));
        assertEquals(true, response.has("rating"));
        assertEquals(false, response.has("cache_hit"));
        assertEquals(true, response.get("cacheHit").isNull());
        assertEquals(true, response.get("rating").isNull());
    }
}
