package apiservice.controller;

import apiservice.repository.ChatInteractionRepository;
import apiservice.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ChatInteractionRepository chatInteractionRepository;
    private static final int RECENT_INTERACTIONS_LIMIT = 100;

    public ChatController(ChatService chatService, ChatInteractionRepository chatInteractionRepository) {
        this.chatService = chatService;
        this.chatInteractionRepository = chatInteractionRepository;
    }

    @GetMapping("/recent-interactions")
    public ResponseEntity<RecentChatbotInteractionsResponse> getRecentChatbotInteractions() {
        var recentInteractions = chatInteractionRepository.findByOrderByCreatedAtDesc(
                        PageRequest.of(0, RECENT_INTERACTIONS_LIMIT)
                ).stream()
                .map(interaction -> new ChatbotInteractionResponse(
                        interaction.getQuestion(),
                        interaction.getAnswer(),
                        interaction.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(new RecentChatbotInteractionsResponse(recentInteractions));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required.");
        }

        String answer = chatService.ask(request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    public record ChatRequest(@NotBlank(message = "Question is required") String question) {
    }

    public record ChatResponse(String answer) {
    }
}
