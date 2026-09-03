package apiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class ContactService {
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
            "(<\\s*/?\\s*script\\b)|(<[^>]+>)|((?m)^\\s*(bcc|cc|to|from|subject)\\s*:)|(%0d|%0a)|([\\r\\n])",
            Pattern.CASE_INSENSITIVE
    );

    private final JavaMailSender mailSender;

    @Value("${app.contact.to-email}")
    private String toEmail;

    @Value("${app.contact.from-email}")
    private String fromEmail;

    @Value("${app.contact.subject}")
    private String subject;

    public ContactService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactEmail(String messageText, String subjectLine) {
        validateContent(messageText);
        validateSubject(subjectLine);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject(subjectLine);
        message.setText(messageText);
        mailSender.send(message);
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message exceeds max length of " + MAX_MESSAGE_LENGTH + " characters."
            );
        }

        if (MALICIOUS_PATTERN.matcher(content).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message contains disallowed patterns.");
        }
    }

    private void validateSubject(String subjectLine) {
        if (subjectLine == null || subjectLine.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required.");
        }

        if (subjectLine.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject exceeds max length of 200 characters.");
        }

        if (MALICIOUS_PATTERN.matcher(subjectLine).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject contains disallowed patterns.");
        }
    }
}
