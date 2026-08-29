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

@Service
public class ChatServiceImpl implements ChatService {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.chat.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.chat.openai-embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${app.chat.openai-chat-model:gpt-4o-mini}")
    private String chatModel;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.vector-index:portfolio_documents_vector_index}")
    private String vectorIndex;

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
            float[] queryVector = embedText(trimmedQuestion);
            List<PortfolioDocument> relevantDocuments = findRelevantDocuments(queryVector);
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

    private List<PortfolioDocument> findRelevantDocuments(float[] queryVector) {
        List<Double> vector = new ArrayList<>();
        for (float value : queryVector) {
            vector.add((double) value);
        }

        List<Document> pipeline = List.of(
                new Document("$vectorSearch", new Document("index", vectorIndex)
                        .append("path", "embedding")
                        .append("queryVector", vector)
                        .append("numCandidates", Math.max(maxContextChunks * 10, 20))
                        .append("limit", maxContextChunks)),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1)
                        .append("score", new Document("$meta", "vectorSearchScore")))
        );

        List<PortfolioDocument> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            PortfolioDocument chunk = new PortfolioDocument();
            chunk.setId(document.getString("_id"));
            chunk.setText(document.getString("text"));
            chunk.setMetadata(document.get("metadata", Map.class));
            chunk.setScore(document.get("score", Number.class) == null ? 0.0 : document.get("score", Number.class).doubleValue());
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

    private float[] embedText(String text) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
                "input", text,
                "model", embeddingModel
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI embeddings request failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode embeddingNode = json.path("data").get(0).path("embedding");
        float[] embedding = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding[i] = (float) embeddingNode.get(i).asDouble();
        }
        return embedding;
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
