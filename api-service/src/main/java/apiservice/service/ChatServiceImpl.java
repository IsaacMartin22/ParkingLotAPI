package apiservice.service;

import apiservice.model.PortfolioDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ChatServiceImpl implements ChatService {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.chat.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.chat.openai-chat-model:gpt-4o-mini}")
    private String chatModel;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.max-context-chunks:5}")
    private int maxContextChunks;

    @Value("${app.chat.system-prompt:You are Isaac Martin's portfolio assistant. Answer recruiter and hiring manager questions using only the provided context. If the answer is not in the context, say you do not have enough information and invite them to ask about skills, experience, or projects.}")
    private String systemPrompt;

    public ChatServiceImpl(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String ask(String question) {
        String trimmedQuestion = question == null ? "" : question.trim();
        if (trimmedQuestion.isBlank()) {
            return "Please provide a question about Isaac's background, skills, or experience.";
        }

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return "The portfolio chatbot is not configured yet. Set OPENAI_API_KEY and MONGODB_URI before asking questions.";
        }

        try {
            List<PortfolioDocument> relevantDocuments = findRelevantDocuments(trimmedQuestion);
            String context = buildContext(relevantDocuments);

            if (context.isBlank()) {
                return "I could not find enough relevant information in Isaac's portfolio data for that question. " +
                        "Ask about his Java work, backend systems, product experience, or technical background.";
            }

            return chatCompletion(trimmedQuestion, context);
        } catch (Exception ex) {
            return "I hit a problem while answering that question. Please try again or ask a simpler question about Isaac's professional background. " +
                    ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private List<PortfolioDocument> findRelevantDocuments(String queryText) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("text", new Document("$regex", Pattern.quote(queryText)).append("$options", "i"))),
                new Document("$limit", maxContextChunks),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1))
        );

        List<PortfolioDocument> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            PortfolioDocument chunk = new PortfolioDocument();
            chunk.setId(document.getString("_id"));
            chunk.setText(document.getString("text"));
            chunk.setMetadata((Map<String, Object>) (Map<?, ?>) document.get("metadata", Map.class));
            results.add(chunk);
        }

        if (!results.isEmpty()) {
            return results;
        }

        return findRelevantDocumentsFallback(queryText);
    }

    @SuppressWarnings("unchecked")
    private List<PortfolioDocument> findRelevantDocumentsFallback(String queryText) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("text", new Document("$regex", "(" + Pattern.quote(queryText.split(" ")[0]) + ")").append("$options", "i"))),
                new Document("$limit", maxContextChunks),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1))
        );

        List<PortfolioDocument> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            PortfolioDocument chunk = new PortfolioDocument();
            chunk.setId(document.getString("_id"));
            chunk.setText(document.getString("text"));
            chunk.setMetadata((Map<String, Object>) (Map<?, ?>) document.get("metadata", Map.class));
            results.add(chunk);
        }
        return results;
    }

    private String buildContext(List<PortfolioDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (PortfolioDocument document : documents) {
            if (document == null || document.getText() == null || document.getText().isBlank()) {
                continue;
            }
            context.append(document.getText()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return context.toString().trim();
    }

    private String chatCompletion(String question, String context) throws IOException, InterruptedException {
        String userContent = "Question: " + question + System.lineSeparator() + System.lineSeparator() +
                "Relevant context:" + System.lineSeparator() + context;

        String body = objectMapper.writeValueAsString(Map.of(
                "model", chatModel,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                )
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI chat request failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.path("choices").get(0).path("message").path("content").asText();
    }
}
