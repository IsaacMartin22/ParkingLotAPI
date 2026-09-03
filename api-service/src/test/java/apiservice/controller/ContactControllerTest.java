package apiservice.controller;

import apiservice.service.ContactService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContactControllerTest {

    @Test
    void sendContactEmailReturnsAcceptedAndDelegatesToService() {
        ContactService contactService = mock(ContactService.class);
        ContactController controller = new ContactController(contactService);

        var response = controller.sendContactEmail(new ContactController.ContactRequest("Hello there"));

        assertEquals(202, response.getStatusCode().value());
        verify(contactService).sendContactEmail("Hello there");
    }
}
