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
    private static final String TARGET_LIST_NAME = "Feature Requests";
    private static final Set<String> ARGUMENTS = Set.of("name", "description");

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
                "Create a new feature request Trello card on the server's configured board. Use only when the user requests "
                        + "a new feature. The card always goes in the open column named Feature Requests. "
                        + "Supply a card name and an optional description. "
                        + "This writes to Trello; do not retry automatically after an uncertain outcome.",
                """
                {
                  "type": "object",
                  "properties": {
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
                  "required": ["name"],
                  "additionalProperties": false
                }
                """
        ), (exchange, arguments) -> call(arguments));
    }

    McpSchema.CallToolResult call(Map<String, Object> arguments) {
        String errorId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        String stage = "arguments";
        boolean creationAttempted = false;
        logger.info("MCP tool call started. tool={}, errorId={}", NAME, errorId);
        try {
            if (arguments == null || !ARGUMENTS.containsAll(arguments.keySet())
                    || !(arguments.get("name") instanceof String name) || name.isBlank() || name.length() > 16384
                    || (arguments.containsKey("description")
                    && (!(arguments.get("description") instanceof String description) || description.length() > 16384))) {
                throw new IllegalArgumentException(
                        "Supply a non-blank name and an optional string description; "
                                + "name and description must not exceed 16384 characters. No other arguments are accepted.");
            }
            stage = "configuration";
            boolean apiKeyPresent = apiKey != null && !apiKey.isBlank();
            boolean apiTokenPresent = apiToken != null && !apiToken.isBlank();
            boolean boardIdValid = boardId != null && boardId.matches("[0-9a-fA-F]{24}");
            if (!apiKeyPresent || !apiTokenPresent || !boardIdValid) {
                logger.warn("MCP tool unavailable. tool={}, errorCode=CONFIGURATION_ERROR, errorId={}, "
                                + "apiKeyPresent={}, apiTokenPresent={}, boardIdValid={}",
                        NAME, errorId, apiKeyPresent, apiTokenPresent, boardIdValid);
                return error("Configure TRELLO_API_KEY, TRELLO_API_TOKEN, and TRELLO_BOARD_ID (full 24-character ID).",
                        "CONFIGURATION_ERROR", errorId);
            }

            stage = "list_lookup";
            String listId = findFeatureRequestsListId(errorId);

            stage = "card_request";
            String body = objectMapper.writeValueAsString(Map.of(
                    "idList", listId, "name", name.trim(), "desc", arguments.getOrDefault("description", "")));
            HttpRequest createRequest = request("/cards")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            // Never retry a write: a lost response can still mean the card was created.
            creationAttempted = true;
            stage = "card_creation";
            JsonNode card = send(createRequest, errorId, stage);
            stage = "card_response";
            if (!card.path("id").isTextual() || card.path("id").asText().isBlank()
                    || !card.path("url").isTextual() || card.path("url").asText().isBlank()) {
                throw new IOException("Invalid Trello card response");
            }
            McpSchema.CallToolResult result = new McpSchema.CallToolResult(objectMapper.writeValueAsString(Map.of(
                    "id", card.path("id").asText(), "url", card.path("url").asText(),
                    "name", name.trim(), "listId", listId, "boardId", boardId)), false);
            logger.info("MCP tool call completed. tool={}, errorId={}, durationMs={}",
                    NAME, errorId, (System.nanoTime() - startedAt) / 1_000_000);
            return result;
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP tool call rejected. tool={}, errorCode=INVALID_ARGUMENTS, errorId={}", NAME, errorId);
            return error("Invalid arguments: " + ex.getMessage(), "INVALID_ARGUMENTS", errorId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(creationAttempted, errorId, stage, startedAt, ex);
        } catch (IOException ex) {
            return failure(creationAttempted, errorId, stage, startedAt, ex);
        }
    }

    private String findFeatureRequestsListId(String errorId) throws IOException, InterruptedException {
        JsonNode lists = send(request("/boards/" + boardId + "/lists?filter=open&fields=id,name,idBoard,closed")
                .GET().build(), errorId, "list_lookup");
        if (!lists.isArray()) {
            logger.warn("Trello list lookup failed. tool={}, errorId={}, reason=EXPECTED_LIST_ARRAY", NAME, errorId);
            throw new IOException("Invalid Trello lists response");
        }
        for (JsonNode list : lists) {
            if (TARGET_LIST_NAME.equals(list.path("name").asText())
                    && list.path("id").asText().matches("[0-9a-fA-F]{24}")
                    && boardId.equalsIgnoreCase(list.path("idBoard").asText())
                    && list.path("closed").isBoolean() && !list.path("closed").asBoolean()) {
                return list.path("id").asText();
            }
        }
        logger.warn("Trello list lookup failed. tool={}, errorId={}, reason=TARGET_LIST_NOT_FOUND, listCount={}",
                NAME, errorId, lists.size());
        throw new IOException("Feature Requests column not found on configured board");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("https://api.trello.com/1" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Authorization", "OAuth oauth_consumer_key=\"" + encode(apiKey)
                        + "\", oauth_token=\"" + encode(apiToken) + "\"");
    }

    private JsonNode send(HttpRequest request, String errorId, String stage) throws IOException, InterruptedException {
        long startedAt = System.nanoTime();
        logger.info("Trello request started. tool={}, errorId={}, stage={}, method={}",
                NAME, errorId, stage, request.method());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Trello response received. tool={}, errorId={}, stage={}, method={}, httpStatus={}, durationMs={}",
                NAME, errorId, stage, request.method(), response.statusCode(),
                (System.nanoTime() - startedAt) / 1_000_000);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn("Trello request failed. tool={}, errorId={}, stage={}, httpStatus={}",
                    NAME, errorId, stage, response.statusCode());
            throw new IOException("Trello request failed");
        }
        JsonNode json = objectMapper.readTree(response.body());
        if (json == null || !(json.isObject() || json.isArray())) {
            logger.warn("Trello response invalid. tool={}, errorId={}, stage={}, reason=EXPECTED_OBJECT_OR_ARRAY",
                    NAME, errorId, stage);
            throw new IOException("Invalid Trello response");
        }
        return json;
    }

    private McpSchema.CallToolResult failure(
            boolean creationAttempted, String errorId, String stage, long startedAt, Exception exception
    ) {
        // Upstream bodies and exceptions can contain credentials or private card content.
        logger.error("MCP tool call failed. tool={}, errorCode=TRELLO_ERROR, errorId={}, stage={}, "
                        + "creationAttempted={}, exceptionType={}, durationMs={}",
                NAME, errorId, stage, creationAttempted, exception.getClass().getSimpleName(),
                (System.nanoTime() - startedAt) / 1_000_000);
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
