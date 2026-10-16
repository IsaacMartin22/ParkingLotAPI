package apiservice.service;

import apiservice.dbentity.ChatInteraction;
import apiservice.dbentity.ChatToolUsage;
import apiservice.model.OpenAiChatModel;
import apiservice.repository.ChatInteractionRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
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
    private final ChatInteractionRepository chatInteractionRepository = mock(ChatInteractionRepository.class);

    @Test
    void recordsEachToolUsageWithoutToolPayloads() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        String diagnostic = "[MCP errorCode=DATABASE_ERROR errorId=577daf18-8abc-4932-9981-66f9382cd133]";
        when(mcpClient.callTool(any()))
                .thenReturn(new McpSchema.CallToolResult("private-tool-result", false))
                .thenReturn(new McpSchema.CallToolResult("private-tool-result " + diagnostic, true));
        respondWith(toolCall("tool_one"), toolCall("tool_one"), assistantMessage("Answer"));
        List<ChatToolUsage> usages = new java.util.ArrayList<>();

        assertEquals("Answer", ReflectionTestUtils.invokeMethod(
                service, "chatCompletion", "Question", "Context", "gpt-6.1-sol", usages));

        assertEquals(2, usages.size());
        assertEquals("tool_one", usages.get(0).getToolName());
        assertEquals("call_1", usages.get(0).getCallId());
        assertEquals(1, usages.get(0).getToolRound());
        assertFalse(usages.get(0).getError());
        assertEquals(null, usages.get(0).getErrorDiagnostic());
        assertEquals(2, usages.get(1).getToolRound());
        assertTrue(usages.get(1).getError());
        assertEquals(diagnostic, usages.get(1).getErrorDiagnostic());
    }

    @Test
    void savesInteractionWithLinkedToolUsages() {
        ChatServiceImpl service = service();
        ReflectionTestUtils.setField(service, "embeddingModel", "text-embedding-3-large");
        ChatToolUsage usage = new ChatToolUsage();
        usage.setToolName("tool_one");

        ReflectionTestUtils.invokeMethod(service, "saveInteraction", "Question", "Answer",
                new float[]{0.5f, -1.0f}, "gpt-6.1-sol", 10L, 20L, 3, List.of(usage));

        var saved = ArgumentCaptor.forClass(ChatInteraction.class);
        verify(chatInteractionRepository).save(saved.capture());
        ChatInteraction interaction = saved.getValue();
        assertEquals("Question", interaction.getQuestion());
        assertEquals("Answer", interaction.getAnswer());
        assertEquals("[0.5,-1.0]", interaction.getEmbedding());
        assertEquals("gpt-6.1-sol", interaction.getChatModel());
        assertEquals(3, interaction.getVectorSearchDocumentCount());
        assertEquals(List.of(usage), interaction.getToolUsages());
        assertSame(interaction, usage.getChatInteraction());
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void logsToolStartAndOutcomeWithoutToolPayloads(boolean isError) throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        when(mcpClient.callTool(any()))
                .thenReturn(new McpSchema.CallToolResult("private-tool-result", isError));
        respondWith(toolCall("tool_one").replace("\"arguments\":\"{}\"",
                        "\"arguments\":\"{\\\"secret\\\":\\\"private-tool-argument\\\"}\""),
                assistantMessage("Answer"));
        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals("Answer", complete(service));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("MCP tool call started",
                        "tool=tool_one", "callId=call_1", "model=gpt-6.1-sol", "round=1");
            });
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(isError ? Level.WARN : Level.INFO);
                assertThat(event.getFormattedMessage()).contains(
                        isError ? "MCP tool call returned an error" : "MCP tool call completed",
                        "tool=tool_one", "callId=call_1", "durationMs=");
            });
            assertThat(appender.list).allSatisfy(event ->
                    assertThat(event.getFormattedMessage())
                            .doesNotContain("private-tool-result", "private-tool-argument"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void logsServerDiagnosticWithoutLoggingErrorPayload() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        String diagnostic = "[MCP errorCode=DATABASE_ERROR errorId=577daf18-8abc-4932-9981-66f9382cd133]";
        when(mcpClient.callTool(any()))
                .thenReturn(new McpSchema.CallToolResult("private-tool-result " + diagnostic, true));
        respondWith(toolCall("tool_one"), assistantMessage("Tool unavailable"));
        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals("Tool unavailable", complete(service));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("callId=call_1", diagnostic);
            });
            assertThat(appender.list).allSatisfy(event ->
                    assertThat(event.getFormattedMessage()).doesNotContain("private-tool-result"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "42P01, Analytics table not found",
            "42703, Analytics column not found",
            "42501, Database permission denied",
            "3D000, Database not found",
            "3F000, Schema not found",
            "57014, Database query canceled or timed out",
            "08001, Database connection failed",
            "28P01, Database authentication failed",
            "XX000, Check the MCP server stack trace",
            "unknown, verify the active profile's datasource variables"
    })
    void logsDatabaseSqlStateAndActionableHint(String sqlState, String hint) throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        String diagnostic = "[MCP errorCode=DATABASE_ERROR sqlState=" + sqlState
                + " errorId=577daf18-8abc-4932-9981-66f9382cd133]";
        when(mcpClient.callTool(any()))
                .thenReturn(new McpSchema.CallToolResult("private-tool-result " + diagnostic, true));
        respondWith(toolCall("tool_one"), assistantMessage("Tool unavailable"));
        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals("Tool unavailable", complete(service));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("callId=call_1", diagnostic, "hint=", hint);
            });
            assertThat(appender.list).allSatisfy(event ->
                    assertThat(event.getFormattedMessage()).doesNotContain("private-tool-result"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void doesNotLogUntrustedDatabaseDiagnosticFields() {
        String diagnostic = "[MCP errorCode=DATABASE_ERROR sqlState=private-value\nforged-log-entry"
                + " errorId=577daf18-8abc-4932-9981-66f9382cd133]";
        String logged = ReflectionTestUtils.invokeMethod(ChatServiceImpl.class, "toolErrorDiagnostic",
                new McpSchema.CallToolResult(diagnostic, true));

        assertEquals("unavailable (tool returned no diagnostic tag)", logged);
    }

    @Test
    void logsToolExceptionAndPropagatesIt() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        IllegalStateException failure = new IllegalStateException("private-error-details");
        when(mcpClient.callTool(any())).thenThrow(failure);
        respondWith(toolCall("tool_one"));
        Logger logger = (Logger) LoggerFactory.getLogger(ChatServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> complete(service)));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage()).contains("MCP tool call failed",
                        "tool=tool_one", "callId=call_1", "durationMs=", "exceptionType=IllegalStateException");
                assertThat(event.getFormattedMessage()).doesNotContain("private-error-details");
                assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
            });
            assertThat(appender.list).noneSatisfy(event ->
                    assertThat(event.getFormattedMessage()).contains("MCP tool call completed"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @EnumSource(OpenAiChatModel.class)
    void letsModelSelectToolAndSendsErrorResultBackWithoutHidingIt(OpenAiChatModel model) throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        when(mcpClient.callTool(new McpSchema.CallToolRequest("tool_one", Map.of())))
                .thenReturn(new McpSchema.CallToolResult("Not implemented", true));
        respondWith(toolCall("tool_one"), assistantMessage("Tool is unavailable"));

        assertEquals("Tool is unavailable", ReflectionTestUtils.invokeMethod(
                service, "chatCompletion", "Question", "Context", model.getValue()));

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        JsonNode first = body(requests.getAllValues().get(0));
        assertEquals("https://api.openai.com/v1/responses", requests.getAllValues().get(0).uri().toString());
        assertEquals(model.getValue(), first.path("model").asText());
        assertFalse(first.has("reasoning_effort"));
        assertFalse(first.path("store").asBoolean());
        assertEquals("reasoning.encrypted_content", first.path("include").path(0).asText());
        assertEquals("auto", first.path("tool_choice").asText());
        assertEquals("tool_one", first.path("tools").path(0).path("name").asText());
        assertEquals("object", first.path("tools").path(0).path("parameters").path("type").asText());
        assertFalse(first.path("tools").path(0).path("strict").asBoolean());
        JsonNode second = body(requests.getAllValues().get(1));
        assertEquals(model.getValue(), second.path("model").asText());
        assertFalse(second.has("reasoning_effort"));
        assertEquals("encrypted-reasoning", second.path("input").path(2).path("encrypted_content").asText());
        assertEquals("call_1", second.path("input").path(3).path("call_id").asText());
        JsonNode result = second.path("input").path(4);
        assertEquals("function_call_output", result.path("type").asText());
        assertEquals("call_1", result.path("call_id").asText());
        assertEquals(true, objectMapper.readTree(result.path("output").asText()).path("isError").asBoolean());
        verify(mcpClient).callTool(new McpSchema.CallToolRequest("tool_one", Map.of()));
    }

    @ParameterizedTest
    @EnumSource(OpenAiChatModel.class)
    void leavesReasoningDefaultWhenNoToolsAreAdvertised(OpenAiChatModel model) throws Exception {
        ChatServiceImpl service = service();
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(), null));
        respondWith(assistantMessage("Answer"));

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

    @ParameterizedTest
    @EnumSource(OpenAiChatModel.class)
    void stopsToolCallingAfterFourRounds(OpenAiChatModel model) throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        when(mcpClient.callTool(any())).thenReturn(new McpSchema.CallToolResult("Done", false));
        respondWith(toolCall("tool_one"));

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "chatCompletion", "Question", "Context", model.getValue()));

        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(5)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("none", body(requests.getAllValues().get(4)).path("tool_choice").asText());
        for (HttpRequest request : requests.getAllValues()) {
            assertFalse(body(request).has("reasoning_effort"));
        }
        verify(mcpClient, times(4)).callTool(any());
    }

    @Test
    void rejectsResponseWithoutAnswer() throws Exception {
        ChatServiceImpl service = service();
        when(mcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(), null));
        respondWith("");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> complete(service));

        assertEquals("OpenAI returned no answer", error.getMessage());
        verify(mcpClient, times(0)).callTool(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsIncompleteResponseBeforeExecutingTools() throws Exception {
        ChatServiceImpl service = service();
        advertiseTool();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"status\":\"incomplete\",\"output\":[" + toolCall("tool_one") + "]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        assertThrows(IllegalStateException.class, () -> complete(service));

        verify(mcpClient, times(0)).callTool(any());
    }

    @Test
    void missingApiKeyReturnsConfigurationMessageWithoutCallingMcp() {
        ChatServiceImpl service = service();
        ReflectionTestUtils.setField(service, "openAiApiKey", "");

        assertEquals("The portfolio chatbot is not configured yet. ", service.ask("Question"));

        verifyNoInteractions(mcpClient);
    }

    private ChatServiceImpl service() {
        ChatServiceImpl service = new ChatServiceImpl(null, objectMapper, mcpClient, chatInteractionRepository);
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
            when(response.body()).thenReturn("{\"status\":\"completed\",\"output\":[" + message + "]}");
            responses.add(response);
        }
        var stubbing = when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)));
        for (HttpResponse<String> response : responses) {
            stubbing = stubbing.thenReturn(response);
        }
    }

    private String toolCall(String name) {
        return """
                {"id":"rs_1","type":"reasoning","summary":[],"encrypted_content":"encrypted-reasoning"},
                {"id":"fc_1","call_id":"call_1","type":"function_call","name":"%s","arguments":"{}"}
                """.formatted(name);
    }

    private String assistantMessage(String text) {
        return """
                {"type":"message","role":"assistant","content":[{"type":"output_text","text":"%s"}]}
                """.formatted(text);
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
