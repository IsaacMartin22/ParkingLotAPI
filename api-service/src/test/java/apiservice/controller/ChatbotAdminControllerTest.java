package apiservice.controller;

import apiservice.model.PortfolioDocument;
import apiservice.service.PortfolioDocumentSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import parkinglot.common.request.ChatbotSeedRequest;
import parkinglot.common.response.ChatbotSeedResponse;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotAdminControllerTest {

    @Test
    void seedDocumentReturnsSeededResponseWhenAuthorized() throws Exception {
        PortfolioDocumentSeedService seedService = mock(PortfolioDocumentSeedService.class);
        ChatbotAdminController controller = new ChatbotAdminController(seedService);
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
        ChatbotAdminController controller = new ChatbotAdminController(seedService);
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
        ChatbotAdminController controller = new ChatbotAdminController(seedService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.seedDocument(
                        "secret",
                        new ChatbotSeedRequest("Hello world", "category", "source")
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
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
