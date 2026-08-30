package apiservice.service;

import apiservice.model.PortfolioDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

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
        logger.info("ask() called with question='{}'", trimmedQuestion);

        if (trimmedQuestion.isBlank()) {
            logger.warn("ask() received blank question");
            return "Please provide a question about Isaac's background, skills, or experience.";
        }

        logger.info("Config check: openAiApiKeyPresent={}, collectionName={}, maxContextChunks={}",
                openAiApiKey != null && !openAiApiKey.isBlank(), collectionName, maxContextChunks);

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            logger.error("ask() aborted: OPENAI_API_KEY is missing");
            return "The portfolio chatbot is not configured yet. Set OPENAI_API_KEY and MONGODB_URI before asking questions.";
        }

        try {
            logger.info("Searching MongoDB for relevant documents for question='{}'", trimmedQuestion);
            List<PortfolioDocument> relevantDocuments = findRelevantDocuments(trimmedQuestion);
            logger.info("Retrieved {} candidate documents from MongoDB for question='{}'",
                    relevantDocuments.size(), trimmedQuestion);

            String context = buildContext(relevantDocuments);
            logger.info("Built context length={} chars for question='{}'", context.length(), trimmedQuestion);
            if (context.length() > 200) {
                logger.info("Context preview='{}'", context.substring(0, 200));
            }

            if (context.isBlank()) {
                logger.warn("No relevant context found for question='{}'. Returning fallback response.", trimmedQuestion);
                return "I could not find enough relevant information in Isaac's portfolio data for that question. " +
                        "Ask about his Java work, backend systems, product experience, or technical background.";
            }

            logger.info("Calling OpenAI chat completion for question='{}' with contextLength={} chars", trimmedQuestion, context.length());
            return chatCompletion(trimmedQuestion, context);
        } catch (Exception ex) {
            logger.error("Exception while handling question='{}'", trimmedQuestion, ex);
            return "I hit a problem while answering that question. Please try again or ask a simpler question about Isaac's professional background. " +
                    ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private List<PortfolioDocument> findRelevantDocuments(String queryText) {
        String exactRegex = Pattern.quote(queryText);
        logger.info("findRelevantDocuments() exact match regex='{}'", exactRegex);

        List<Document> pipeline = List.of(
                new Document("$match", new Document("text", new Document("$regex", exactRegex).append("$options", "i"))),
                new Document("$limit", maxContextChunks),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1))
        );

        List<PortfolioDocument> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            PortfolioDocument chunk = new PortfolioDocument();
            Object idValue = document.get("_id");
            chunk.setId(idValue == null ? null : idValue.toString());
            chunk.setText(document.getString("text"));
            chunk.setMetadata((Map<String, Object>) (Map<?, ?>) document.get("metadata", Map.class));
            results.add(chunk);
        }

        logger.info("findRelevantDocuments() exact match result count={}", results.size());
        if (!results.isEmpty()) {
            return results;
        }

        return findRelevantDocumentsFallback(queryText);
    }

    @SuppressWarnings("unchecked")
    private List<PortfolioDocument> findRelevantDocumentsFallback(String queryText) {
        String firstToken = queryText.split(" ")[0];
        String fallbackRegex = "(" + Pattern.quote(firstToken) + ")";
        logger.warn("findRelevantDocumentsFallback() regex='{}' using first token='{}'", fallbackRegex, firstToken);

        List<Document> pipeline = List.of(
                new Document("$match", new Document("text", new Document("$regex", fallbackRegex).append("$options", "i"))),
                new Document("$limit", maxContextChunks),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1))
        );

        List<PortfolioDocument> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            PortfolioDocument chunk = new PortfolioDocument();
            Object idValue = document.get("_id");
            chunk.setId(idValue == null ? null : idValue.toString());
            chunk.setText(document.getString("text"));
            chunk.setMetadata((Map<String, Object>) (Map<?, ?>) document.get("metadata", Map.class));
            results.add(chunk);
        }
        logger.info("findRelevantDocumentsFallback() result count={}", results.size());
        return results;
    }

    private String buildContext(List<PortfolioDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            logger.warn("buildContext() received null/empty documents list");
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (PortfolioDocument document : documents) {
            if (document == null || document.getText() == null || document.getText().isBlank()) {
                logger.warn("Skipping empty/null portfolio document in buildContext");
                continue;
            }
            context.append(document.getText()).append(System.lineSeparator()).append(System.lineSeparator());
        }

        String result = context.toString().trim();
        logger.info("buildContext() produced {} chars from {} docs", result.length(), documents.size());
        return result;
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

        logger.info("Sending chat completion request. model={}, question='{}', contextLength={}", chatModel, question, context.length());

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("OpenAI chat response status={}, bodyLength={}", response.statusCode(), response.body() == null ? 0 : response.body().length());
        if (response.statusCode() >= 400) {
            logger.error("OpenAI chat request failed with status {}: {}", response.statusCode(), response.body());
            throw new IllegalStateException("OpenAI chat request failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String answer = json.path("choices").get(0).path("message").path("content").asText();
        logger.info("Received answer length={} chars from OpenAI", answer.length());
        return answer;
    }
}
