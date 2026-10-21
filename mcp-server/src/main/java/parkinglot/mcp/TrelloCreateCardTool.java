package parkinglot.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class TrelloCreateCardTool {

    static final String NAME = "create_trello_card";
    private static final Logger logger = LoggerFactory.getLogger(TrelloCreateCardTool.class);
    private static final Set<String> ARGUMENTS = Set.of("listId", "name", "description");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${app.trello.api-key:}")
    private String apiKey;

    @Value("${app.trello.api-token:}")
    private String apiToken;

    @Value("${app.trello.board-id:}")
    private String boardId;

    public TrelloCreateCardTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    McpServerFeatures.SyncToolSpecification specification() {
        return new McpServerFeatures.SyncToolSpecification(new McpSchema.Tool(
                NAME,
                "Create a new Trello card on the server's configured board. Use only when the user requests "
                        + "card creation. Supply the target column's Trello list ID, a card name, and an optional "
                        + "description. Only open columns belonging to the configured board are accepted. "
                        + "This writes to Trello; do not retry automatically after an uncertain outcome.",
                """
                {
                  "type": "object",
                  "properties": {
                    "listId": {
                      "type": "string",
                      "pattern": "^[0-9a-fA-F]{24}$",
                      "description": "Trello list (column) ID on the configured board."
                    },
                    "name": {
                      "type": "string",
                      "minLength": 1,
                      "maxLength": 16384,
                      "description": "Name of the new card."
                    },
                    "description": {
                      "type": "string",
                      "maxLength": 16384,
                      "description": "Optional card description."
                    }
                  },
                  "required": ["listId", "name"],
                  "additionalProperties": false
                }
                """
        ), (exchange, arguments) -> call(arguments));
    }

    McpSchema.CallToolResult call(Map<String, Object> arguments) {
        String errorId = UUID.randomUUID().toString();
        boolean creationAttempted = false;
        try {
            if (arguments == null || !ARGUMENTS.containsAll(arguments.keySet())
                    || !(arguments.get("listId") instanceof String listId)
                    || !listId.matches("[0-9a-fA-F]{24}")
                    || !(arguments.get("name") instanceof String name) || name.isBlank() || name.length() > 16384
                    || (arguments.containsKey("description")
                    && (!(arguments.get("description") instanceof String description) || description.length() > 16384))) {
                throw new IllegalArgumentException(
                        "Supply listId (24 hexadecimal characters), a non-blank name, and an optional string description; "
                                + "name and description must not exceed 16384 characters. No other arguments are accepted.");
            }
            if (apiKey == null || apiKey.isBlank() || apiToken == null || apiToken.isBlank()
                    || boardId == null || !boardId.matches("[0-9a-fA-F]{24}")) {
                logger.warn("MCP tool unavailable. tool={}, errorCode=CONFIGURATION_ERROR, errorId={}", NAME, errorId);
                return error("Configure TRELLO_API_KEY, TRELLO_API_TOKEN, and TRELLO_BOARD_ID (full 24-character ID).",
                        "CONFIGURATION_ERROR", errorId);
            }

            JsonNode list = send(request("/lists/" + listId + "?fields=idBoard,closed").GET().build());
            if (!list.path("idBoard").isTextual() || !list.path("closed").isBoolean()) {
                throw new IOException("Invalid Trello list response");
            }
            if (!boardId.equalsIgnoreCase(list.path("idBoard").asText()) || list.path("closed").asBoolean()) {
                throw new IllegalArgumentException("listId must identify an open column on the configured board");
            }

            String body = objectMapper.writeValueAsString(Map.of(
                    "idList", listId, "name", name.trim(), "desc", arguments.getOrDefault("description", "")));
            HttpRequest createRequest = request("/cards")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            // Never retry a write: a lost response can still mean the card was created.
            creationAttempted = true;
            JsonNode card = send(createRequest);
            if (!card.path("id").isTextual() || card.path("id").asText().isBlank()
                    || !card.path("url").isTextual() || card.path("url").asText().isBlank()) {
                throw new IOException("Invalid Trello card response");
            }
            McpSchema.CallToolResult result = new McpSchema.CallToolResult(objectMapper.writeValueAsString(Map.of(
                    "id", card.path("id").asText(), "url", card.path("url").asText(),
                    "name", name.trim(), "listId", listId, "boardId", boardId)), false);
            logger.info("MCP tool call completed. tool={}", NAME);
            return result;
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP tool call rejected. tool={}, errorCode=INVALID_ARGUMENTS, errorId={}", NAME, errorId);
            return error("Invalid arguments: " + ex.getMessage(), "INVALID_ARGUMENTS", errorId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(creationAttempted, errorId);
        } catch (IOException ex) {
            return failure(creationAttempted, errorId);
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("https://api.trello.com/1" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Authorization", "OAuth oauth_consumer_key=\"" + encode(apiKey)
                        + "\", oauth_token=\"" + encode(apiToken) + "\"");
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn("Trello request failed. tool={}, httpStatus={}", NAME, response.statusCode());
            throw new IOException("Trello request failed");
        }
        JsonNode json = objectMapper.readTree(response.body());
        if (json == null || !json.isObject()) {
            throw new IOException("Invalid Trello response");
        }
        return json;
    }

    private McpSchema.CallToolResult failure(boolean creationAttempted, String errorId) {
        // Upstream bodies and exceptions can contain credentials or private card content.
        logger.error("MCP tool call failed. tool={}, errorCode=TRELLO_ERROR, errorId={}, creationAttempted={}",
                NAME, errorId, creationAttempted);
        return error(creationAttempted
                        ? "Trello card creation could not be confirmed. Check the board before retrying to avoid duplicates."
                        : "Could not verify the Trello column. No card creation was attempted.",
                "TRELLO_ERROR", errorId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A");
    }

    private static McpSchema.CallToolResult error(String message, String code, String errorId) {
        return new McpSchema.CallToolResult(
                message + " [MCP errorCode=" + code + " errorId=" + errorId + "]", true);
    }
}
