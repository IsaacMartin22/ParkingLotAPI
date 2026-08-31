package apiservice.service;

import apiservice.model.PortfolioDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioDocumentSeedService {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.chat.openai-embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    public PortfolioDocumentSeedService(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public int seedMissingDocuments(List<PortfolioDocument> documents) throws IOException, InterruptedException {
        ensureCollectionExists();

        int insertedCount = 0;
        for (PortfolioDocument document : documents) {
            insertedCount += upsertDocument(document) ? 1 : 0;
        }
        return insertedCount;
    }

    public PortfolioDocument addDocument(String text, String category, String source) throws IOException, InterruptedException {
        PortfolioDocument document = buildDocument(text, category, source);
        boolean inserted = upsertDocument(document);
        document.getMetadata().put("seedStatus", inserted ? "seeded" : "existing");
        return document;
    }

    private boolean upsertDocument(PortfolioDocument document) throws IOException, InterruptedException {
        String normalizedText = normalizeText(document.getText());
        String documentId = buildDocumentId(normalizedText);
        document.setId(documentId);

        if (mongoTemplate.exists(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(documentId)
                ),
                collectionName
        )) {
            return false;
        }

        document.setText(normalizedText);
        document.setEmbedding(embedText(normalizedText));
        mongoTemplate.insert(document, collectionName);
        return true;
    }

    private void ensureCollectionExists() {
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName);
        }
    }

    private List<Double> embedText(String text) throws IOException, InterruptedException {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY must be configured to seed chatbot documents.");
        }

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

        JsonNode embeddingNode = objectMapper.readTree(response.body()).path("data").get(0).path("embedding");
        List<Double> embedding = new ArrayList<>(embeddingNode.size());
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding.add(embeddingNode.get(i).asDouble());
        }
        return embedding;
    }

    private PortfolioDocument buildDocument(String text, String category, String source) {
        PortfolioDocument document = new PortfolioDocument();
        document.setText(text);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", category);
        metadata.put("source", source);
        metadata.put("type", "work_experience");
        document.setMetadata(metadata);
        return document;
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String buildDocumentId(String normalizedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedText.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
