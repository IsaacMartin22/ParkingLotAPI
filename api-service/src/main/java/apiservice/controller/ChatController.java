package apiservice.controller;

import apiservice.model.OpenAiChatModel;
import apiservice.repository.ChatInteractionRepository;
import apiservice.service.ChatAnswer;
import apiservice.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

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
                        interaction.getCreatedAt(),
                        interaction.getCacheHit(),
                        interaction.getEmbeddingLatencyMs(),
                        interaction.getVectorSearchDurationMs(),
                        interaction.getVectorSearchDocumentCount(),
                        interaction.getRating()
                ))
                .toList();

        return ResponseEntity.ok(new RecentChatbotInteractionsResponse(recentInteractions));
    }

    @GetMapping("/chat/models")
    public ResponseEntity<AvailableModelsResponse> getAvailableModels() {
        List<String> models = Arrays.stream(OpenAiChatModel.values())
                .map(OpenAiChatModel::getValue)
                .sorted()
                .toList();

        return ResponseEntity.ok(new AvailableModelsResponse(models));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        validateChatRequest(request);

        try {
            String answer = chatService.ask(request.question(), request.model());
            return ResponseEntity.ok(new ChatResponse(answer));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/chat-with-citation")
    public ResponseEntity<ChatWithCitationResponse> chatWithCitation(@Valid @RequestBody ChatRequest request) {
        validateChatRequest(request);

        try {
            ChatAnswer answer = chatService.askWithCitation(request.question(), request.model());
            return ResponseEntity.ok(new ChatWithCitationResponse(answer.answer(), answer.citation()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private void validateChatRequest(ChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required.");
        }

        if (request.model() != null && !request.model().isBlank()) {
            OpenAiChatModel.fromValue(request.model());
        }
    }

    public record ChatRequest(@NotBlank(message = "Question is required") String question, String model) {
    }

    public record ChatResponse(String answer) {
    }

    public record ChatWithCitationResponse(String answer, String citation) {
    }

    public record AvailableModelsResponse(List<String> models) {
    }
}
