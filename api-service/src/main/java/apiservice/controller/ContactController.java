package apiservice.controller;

import apiservice.service.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> sendContactEmail(@Valid @RequestBody ContactRequest request) {
        contactService.sendContactEmail(request.content());
        return ResponseEntity.accepted().build();
    }

    public record ContactRequest(@NotBlank(message = "Content is required") String content) {
    }
}
