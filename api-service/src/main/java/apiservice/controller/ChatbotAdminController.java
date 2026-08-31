package apiservice.controller;

import apiservice.model.PortfolioDocument;
import apiservice.service.PortfolioDocumentSeedService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import parkinglot.common.request.ChatbotSeedRequest;
import parkinglot.common.response.ChatbotSeedResponse;

import java.io.IOException;


// This has not been used yet because I want all seed data to be publicly available to someone
// who wants to see the data
@RestController
@RequestMapping("/api/chatbot/admin")
public class ChatbotAdminController {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final PortfolioDocumentSeedService portfolioDocumentSeedService;

    @Value("${app.chat.admin-token:}")
    private String adminToken;

    public ChatbotAdminController(PortfolioDocumentSeedService portfolioDocumentSeedService) {
        this.portfolioDocumentSeedService = portfolioDocumentSeedService;
    }

    @PostMapping("/seed")
    public ResponseEntity<ChatbotSeedResponse> seedDocument(
            @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String requestToken,
            @Valid @RequestBody ChatbotSeedRequest request
    ) throws IOException, InterruptedException {
        authorize(requestToken);

        PortfolioDocument document = portfolioDocumentSeedService.addDocument(
                request.text(),
                request.category(),
                request.source()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ChatbotSeedResponse(
                        document.getId(),
                        String.valueOf(document.getMetadata().get("seedStatus"))
                ));
    }

    private void authorize(String requestToken) {
        if (!StringUtils.hasText(adminToken)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chatbot admin token is not configured.");
        }
        if (!adminToken.equals(requestToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
