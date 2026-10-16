package apiservice.service;

import apiservice.model.OpenAiChatModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisabledIfSystemProperty(named = "openai.integration", matches = "false")
@Timeout(180)
class OpenAiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static String apiKey;

    @BeforeAll
    static void requireApiKey() {
        apiKey = System.getenv("OPENAI_API_KEY");
        assertNotNull(apiKey, "Live checks require OPENAI_API_KEY in the environment");
        assertFalse(apiKey.isBlank(), "OPENAI_API_KEY must not be blank");
    }

    @ParameterizedTest(name = "{0}: model is available")
    @EnumSource(OpenAiChatModel.class)
    void modelIsAvailable(OpenAiChatModel model) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.openai.com/v1/models/" + model.getValue()))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(),
                () -> model.getValue() + " model lookup failed: " + response.body());
        JsonNode body = OBJECT_MAPPER.readTree(response.body());
        assertEquals(model.getValue(), body.path("id").asText());
    }

    @ParameterizedTest(name = "{0}: plain chat completion")
    @EnumSource(OpenAiChatModel.class)
    void answersWithoutTools(OpenAiChatModel model) {
        McpSyncClient mcpClient = mock(McpSyncClient.class);
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(), null));

        String answer = complete(service(mcpClient), model,
                "Reply with exactly OPENAI_CHAT_OK. Do not add any other text.");

        assertNotNull(answer, model.getValue() + " returned no answer");
        assertEquals("OPENAI_CHAT_OK", answer.trim(), model.getValue());
    }

    @ParameterizedTest(name = "{0}: tool call and follow-up completion")
    @EnumSource(OpenAiChatModel.class)
    void answersUsingToolResult(OpenAiChatModel model) {
        String verificationCode = UUID.randomUUID().toString();
        McpSyncClient mcpClient = mock(McpSyncClient.class);
        McpSchema.Tool tool = new McpSchema.Tool(
                "get_verification_code",
                "Returns the current verification code. Call this tool to learn the code.",
                "{\"type\":\"object\",\"properties\":{},\"required\":[],\"additionalProperties\":false}");
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(tool), null));
        McpSchema.CallToolRequest toolRequest = new McpSchema.CallToolRequest("get_verification_code", Map.of());
        when(mcpClient.callTool(toolRequest))
                .thenReturn(new McpSchema.CallToolResult(verificationCode, false));

        String answer = complete(service(mcpClient), model,
                "Call get_verification_code with no arguments, then reply with exactly the code it returns. "
                        + "The code is only available from the tool. Do not guess it or add other text.");

        verify(mcpClient, atLeastOnce()).callTool(toolRequest);
        assertNotNull(answer, model.getValue() + " returned no answer after tool execution");
        assertTrue(answer.contains(verificationCode),
                () -> model.getValue() + " did not use the tool result: " + answer);
    }

    private ChatServiceImpl service(McpSyncClient mcpClient) {
        ChatServiceImpl service = new ChatServiceImpl(null, OBJECT_MAPPER, mcpClient, null);
        ReflectionTestUtils.setField(service, "openAiApiKey", apiKey);
        ReflectionTestUtils.setField(service, "systemPrompt",
                "You are a synthetic API compatibility test assistant. Follow the user's instructions.");
        return service;
    }

    private String complete(ChatServiceImpl service, OpenAiChatModel model, String question) {
        return ReflectionTestUtils.invokeMethod(service, "chatCompletion", question, "", model.getValue());
    }
}
