package parkinglot.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class PortfolioSearchTool {

    static final String NAME = "search_portfolio_documents";
    private static final Logger logger = LoggerFactory.getLogger(PortfolioSearchTool.class);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.chat.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.chat.openai-embedding-model:text-embedding-3-large}")
    private String embeddingModel;

    @Value("${app.chat.collection:portfolio_documents}")
    private String collectionName;

    @Value("${app.chat.vector-index:portfolio_documents_vector_index}")
    private String vectorIndex;

    @Value("${app.chat.max-context-chunks:5}")
    private int maxContextChunks;

    public PortfolioSearchTool(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    McpServerFeatures.SyncToolSpecification specification() {
        return new McpServerFeatures.SyncToolSpecification(new McpSchema.Tool(
                NAME,
                "Search Isaac Martin's supplied portfolio context. Use for questions about Isaac's background, "
                        + "experience, education, skills, projects, accomplishments, or personal information. "
                        + "Returns relevant document text, metadata (including citations), and relevance scores. "
                        + "An empty result means Isaac has not supplied matching context.",
                """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "minLength": 1,
                      "description": "The question or topic to find in Isaac's portfolio."
                    }
                  },
                  "required": ["query"],
                  "additionalProperties": false
                }
                """
        ), (exchange, arguments) -> call(arguments));
    }

    McpSchema.CallToolResult call(Map<String, Object> arguments) {
        String errorId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        try {
            if (arguments == null || arguments.size() != 1
                    || !(arguments.get("query") instanceof String query) || query.isBlank()) {
                throw new IllegalArgumentException("query must be a non-empty string and the only argument");
            }
            List<Map<String, Object>> documents = search(embedText(query.trim()), query.trim());
            McpSchema.CallToolResult result = new McpSchema.CallToolResult(
                    objectMapper.writeValueAsString(Map.of("documents", documents)), false);
            logger.info("MCP tool call completed. tool={}, durationMs={}, documentCount={}",
                    NAME, (System.nanoTime() - startedAt) / 1_000_000, documents.size());
            return result;
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP tool call rejected. tool={}, errorCode=INVALID_ARGUMENTS, errorId={}", NAME, errorId);
            return error("Invalid arguments: " + ex.getMessage(), "INVALID_ARGUMENTS", errorId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("MCP tool call interrupted. tool={}, errorId={}", NAME, errorId, ex);
            return error("Portfolio search was interrupted.", "INTERNAL_ERROR", errorId);
        } catch (IOException | MongoException | IllegalStateException ex) {
            logger.error("MCP tool call failed. tool={}, errorCode=INTERNAL_ERROR, errorId={}", NAME, errorId, ex);
            return error("Portfolio search is unavailable right now.", "INTERNAL_ERROR", errorId);
        }
    }

    private static McpSchema.CallToolResult error(String message, String code, String errorId) {
        return new McpSchema.CallToolResult(
                message + " [MCP errorCode=" + code + " errorId=" + errorId + "]", true);
    }

    private List<Double> embedText(String text) throws IOException, InterruptedException {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is missing");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                        "input", text, "model", embeddingModel))))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI embeddings request failed with status " + response.statusCode());
        }
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        JsonNode embedding = data.path(0).path("embedding");
        if (!embedding.isArray() || embedding.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no embedding");
        }
        List<Double> vector = new ArrayList<>(embedding.size());
        for (JsonNode value : embedding) {
            if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
                throw new IllegalStateException("OpenAI returned an invalid embedding");
            }
            // Match the float precision used by the original chat retrieval.
            vector.add((double) (float) value.asDouble());
        }
        return vector;
    }

    private List<Map<String, Object>> search(List<Double> vector, String query) {
        List<Map<String, Object>> results = aggregate(List.of(
                new Document("$vectorSearch", new Document("index", vectorIndex)
                        .append("path", "embedding")
                        .append("queryVector", vector)
                        .append("numCandidates", Math.max(maxContextChunks * 10, 20))
                        .append("limit", maxContextChunks)),
                new Document("$project", new Document("_id", 1)
                        .append("text", 1)
                        .append("metadata", 1)
                        .append("score", new Document("$meta", "vectorSearchScore")))
        ), true);
        if (!results.isEmpty()) {
            return results;
        }
        logger.info("No vector matches found; falling back to literal case-insensitive text search.");
        return aggregate(List.of(
                new Document("$match", new Document("text",
                        new Document("$regex", Pattern.quote(query)).append("$options", "i"))),
                new Document("$limit", maxContextChunks),
                new Document("$project", new Document("_id", 1).append("text", 1).append("metadata", 1))
        ), false);
    }

    private List<Map<String, Object>> aggregate(List<Document> pipeline, boolean scored) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document document : mongoTemplate.getCollection(collectionName).aggregate(pipeline)) {
            Map<String, Object> chunk = new LinkedHashMap<>();
            Object id = document.get("_id");
            chunk.put("id", id == null ? null : id.toString());
            chunk.put("text", document.getString("text"));
            chunk.put("metadata", document.get("metadata", Map.class));
            Number score = document.get("score", Number.class);
            chunk.put("score", scored ? (score == null ? 0.0 : score.doubleValue()) : null);
            results.add(chunk);
        }
        return results;
    }
}
