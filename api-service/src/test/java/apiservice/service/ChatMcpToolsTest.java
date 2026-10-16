package apiservice.service;

import apiservice.model.OpenAiChatModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatMcpToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = mock(HttpClient.class);
    private final McpSyncClient mcpClient = mock(McpSyncClient.class);

    @ParameterizedTest
    @EnumSource(OpenAiChatModel.class)
    void letsModelSelectToolAndSendsErrorResultBackWithoutHidingIt(OpenAiChatModel model) throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        when(mcpClient.callTool(new McpSchema.CallToolRequest("tool_one", Map.of())))
                .thenReturn(new McpSchema.CallToolResult("Not implemented", true));
        respondWith(toolCall("tool_one"), "{\"role\":\"assistant\",\"content\":\"Tool is unavailable\"}");

        assertEquals("Tool is unavailable", ReflectionTestUtils.invokeMethod(
                service, "chatCompletion", "Question", "Context", model.getValue()));

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        JsonNode first = body(requests.getAllValues().get(0));
        assertEquals(model.getValue(), first.path("model").asText());
        assertFalse(first.has("reasoning_effort"));
        assertEquals("auto", first.path("tool_choice").asText());
        assertEquals("tool_one", first.path("tools").path(0).path("function").path("name").asText());
        assertEquals("object", first.path("tools").path(0).path("function").path("parameters").path("type").asText());
        JsonNode second = body(requests.getAllValues().get(1));
        assertEquals(model.getValue(), second.path("model").asText());
        assertFalse(second.has("reasoning_effort"));
        assertEquals("call_1", second.path("messages").path(2).path("tool_calls").path(0).path("id").asText());
        JsonNode result = second.path("messages").path(3);
        assertEquals("tool", result.path("role").asText());
        assertEquals("call_1", result.path("tool_call_id").asText());
        assertEquals(true, objectMapper.readTree(result.path("content").asText()).path("isError").asBoolean());
        verify(mcpClient).callTool(new McpSchema.CallToolRequest("tool_one", Map.of()));
    }

    @ParameterizedTest
    @EnumSource(OpenAiChatModel.class)
    void leavesReasoningDefaultWhenNoToolsAreAdvertised(OpenAiChatModel model) throws Exception {
        ChatServiceImpl service = service();
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(), null));
        respondWith("{\"role\":\"assistant\",\"content\":\"Answer\"}");

        assertEquals("Answer", ReflectionTestUtils.invokeMethod(
                service, "chatCompletion", "Question", "Context", model.getValue()));

        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        JsonNode payload = body(request.getValue());
        assertEquals(model.getValue(), payload.path("model").asText());
        assertFalse(payload.has("tools"));
        assertFalse(payload.has("reasoning_effort"));
    }

    @Test
    void rejectsUnadvertisedTools() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        respondWith(toolCall("unknown_tool"));

        assertThrows(IllegalStateException.class, () -> complete(service));
        verify(mcpClient, times(0)).callTool(any());
    }

    @Test
    void stopsToolCallingAfterFourRounds() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        when(mcpClient.callTool(any())).thenReturn(new McpSchema.CallToolResult("Done", false));
        respondWith(toolCall("tool_one"));

        assertThrows(IllegalStateException.class, () -> complete(service));

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(5)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("none", body(requests.getAllValues().get(4)).path("tool_choice").asText());
        for (HttpRequest request : requests.getAllValues()) {
            assertFalse(body(request).has("reasoning_effort"));
        }
        verify(mcpClient, times(4)).callTool(any());
    }

    @Test
    void missingApiKeyReturnsConfigurationMessageWithoutCallingMcp() {
        ChatServiceImpl service = service();
        ReflectionTestUtils.setField(service, "openAiApiKey", "");

        assertEquals("The portfolio chatbot is not configured yet. ", service.ask("Question"));

        verifyNoInteractions(mcpClient);
    }

    private ChatServiceImpl service() {
        ChatServiceImpl service = new ChatServiceImpl(null, objectMapper, mcpClient);
        ReflectionTestUtils.setField(service, "httpClient", httpClient);
        ReflectionTestUtils.setField(service, "openAiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "systemPrompt", "Use portfolio context.");
        return service;
    }

    private void advertiseTool() {
        McpSchema.Tool tool = new McpSchema.Tool("tool_one", "Test tool", "{\"type\":\"object\",\"properties\":{}}");
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(tool), null));
    }

    private String complete(ChatServiceImpl service) {
        return ReflectionTestUtils.invokeMethod(service, "chatCompletion", "Question", "Context", "gpt-6.1-sol");
    }

    @SuppressWarnings("unchecked")
    private void respondWith(String... messages) throws Exception {
        List<HttpResponse<String>> responses = new java.util.ArrayList<>();
        for (String message : messages) {
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn("{\"choices\":[{\"message\":" + message + "}]}");
            responses.add(response);
        }
        var stubbing = when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)));
        for (HttpResponse<String> response : responses) {
            stubbing = stubbing.thenReturn(response);
        }
    }

    private String toolCall(String name) {
        return """
                {"role":"assistant","content":null,"tool_calls":[
                  {"id":"call_1","type":"function","function":{"name":"%s","arguments":"{}"}}
                ]}
                """.formatted(name);
    }

    private JsonNode body(HttpRequest request) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompletableFuture<Void> completed = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }

            public void onError(Throwable error) {
                completed.completeExceptionally(error);
            }

            public void onComplete() {
                completed.complete(null);
            }
        });
        completed.get(5, java.util.concurrent.TimeUnit.SECONDS);
        return objectMapper.readTree(bytes.toString(StandardCharsets.UTF_8));
    }
}
