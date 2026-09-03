package apiservice.controller;

import apiservice.dbentity.ChatInteraction;
import apiservice.model.PortfolioDocument;
import apiservice.repository.ChatInteractionRepository;
import apiservice.service.PortfolioDocumentSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import parkinglot.common.response.ChatbotSeedResponse;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotAdminControllerTest {

    @Test
    void seedDocumentReturnsSeededResponseWhenAuthorized() throws Exception {
        PortfolioDocumentSeedService seedService = mock(PortfolioDocumentSeedService.class);
        ChatInteractionRepository chatInteractionRepository = mock(ChatInteractionRepository.class);
        ChatbotAdminController controller = new ChatbotAdminController(seedService, chatInteractionRepository);
        setAdminToken(controller, "secret");

        PortfolioDocument document = new PortfolioDocument();
        document.setId("doc-123");
        document.setMetadata(new HashMap<>());
        document.getMetadata().put("seedStatus", "seeded");

        when(seedService.addDocument("Hello world", "category", "source")).thenReturn(document);

        var response = controller.seedDocument(
                "secret",
                new ChatbotSeedRequest("Hello world", "category", "source")
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ChatbotSeedResponse responseBody = response.getBody();
        assertEquals("doc-123", responseBody.id());
        assertEquals("seeded", responseBody.status());
    }

    @Test
    void seedDocumentRejectsUnauthorizedRequests() {
        PortfolioDocumentSeedService seedService = mock(PortfolioDocumentSeedService.class);
        ChatInteractionRepository chatInteractionRepository = mock(ChatInteractionRepository.class);
        ChatbotAdminController controller = new ChatbotAdminController(seedService, chatInteractionRepository);
        setAdminToken(controller, "secret");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.seedDocument(
                        "wrong",
                        new ChatbotSeedRequest("Hello world", "category", "source")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void seedDocumentRejectsWhenAdminTokenNotConfigured() {
        PortfolioDocumentSeedService seedService = mock(PortfolioDocumentSeedService.class);
        ChatInteractionRepository chatInteractionRepository = mock(ChatInteractionRepository.class);
        ChatbotAdminController controller = new ChatbotAdminController(seedService, chatInteractionRepository);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.seedDocument(
                        "secret",
                        new ChatbotSeedRequest("Hello world", "category", "source")
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void getRecentChatbotInteractionsReturnsQuestionResponseAndTimestampForMostRecentItems() {
        PortfolioDocumentSeedService seedService = mock(PortfolioDocumentSeedService.class);
        ChatInteractionRepository chatInteractionRepository = mock(ChatInteractionRepository.class);
        ChatbotAdminController controller = new ChatbotAdminController(seedService, chatInteractionRepository);

        ChatInteraction first = new ChatInteraction();
        first.setQuestion("Q1");
        first.setAnswer("A1");
        first.setCreatedAt(Instant.parse("2026-09-03T18:00:00Z"));
        ChatInteraction second = new ChatInteraction();
        second.setQuestion("Q2");
        second.setAnswer("A2");
        second.setCreatedAt(Instant.parse("2026-09-03T17:59:00Z"));

        when(chatInteractionRepository.findByOrderByCreatedAtDesc(any())).thenReturn(List.of(first, second));

        var response = controller.getRecentChatbotInteractions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertEquals(2, body.interactions().size());
        assertEquals("Q1", body.interactions().get(0).question());
        assertEquals("A1", body.interactions().get(0).response());
        assertEquals(Instant.parse("2026-09-03T18:00:00Z"), body.interactions().get(0).timestamp());
        assertEquals("Q2", body.interactions().get(1).question());
        assertEquals("A2", body.interactions().get(1).response());
        assertEquals(Instant.parse("2026-09-03T17:59:00Z"), body.interactions().get(1).timestamp());
    }

    private void setAdminToken(ChatbotAdminController controller, String value) {
        try {
            Field field = ChatbotAdminController.class.getDeclaredField("adminToken");
            field.setAccessible(true);
            field.set(controller, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
