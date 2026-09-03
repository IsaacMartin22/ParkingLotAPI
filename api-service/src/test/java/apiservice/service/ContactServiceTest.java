package apiservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContactServiceTest {

    @Test
    void sendContactEmailSendsMessageWhenContentIsValid() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ContactService service = new ContactService(mailSender);
        ReflectionTestUtils.setField(service, "toEmail", "IsaacMartin151@gmail.com");
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@parkinglot.local");
        ReflectionTestUtils.setField(service, "subject", "Contact");

        service.sendContactEmail("Need help with parking access.");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendContactEmailRejectsScriptContent() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ContactService service = new ContactService(mailSender);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.sendContactEmail("<script>alert('xss')</script>")
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void sendContactEmailRejectsBlankContent() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ContactService service = new ContactService(mailSender);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.sendContactEmail("   ")
        );

        assertEquals(400, exception.getStatusCode().value());
    }
}
