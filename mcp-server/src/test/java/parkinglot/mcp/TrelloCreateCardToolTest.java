package parkinglot.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TrelloCreateCardToolTest {

    private static final String BOARD_ID = "aaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String LIST_ID = "bbbbbbbbbbbbbbbbbbbbbbbb";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = mock(HttpClient.class);
    private final TrelloCreateCardTool tool = new TrelloCreateCardTool(objectMapper);

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(tool, "httpClient", httpClient);
        ReflectionTestUtils.setField(tool, "apiKey", "test-key");
        ReflectionTestUtils.setField(tool, "apiToken", "test-token");
        ReflectionTestUtils.setField(tool, "boardId", BOARD_ID);
    }

    @Test
    void verifiesBoardThenCreatesCardAndReturnsUrl() throws Exception {
        respond(openList(), response(200, "{\"id\":\"card-id\",\"url\":\"https://trello.com/c/card\"}"));

        var result = tool.call(Map.of("name", " Follow up ",
                "description", "Quotes: \"hello\"\nUnicode: \u00e9"));

        assertFalse(Boolean.TRUE.equals(result.isError()), text(result));
        var json = objectMapper.readTree(text(result));
        assertEquals("card-id", json.path("id").asText());
        assertEquals("https://trello.com/c/card", json.path("url").asText());
        assertEquals(BOARD_ID, json.path("boardId").asText());
        assertEquals(LIST_ID, json.path("listId").asText());
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        var get = requests.getAllValues().get(0);
        assertEquals("GET", get.method());
        assertEquals("https://api.trello.com/1/boards/" + BOARD_ID
                + "/lists?filter=open&fields=id,name,idBoard,closed", get.uri().toString());
        var post = requests.getAllValues().get(1);
        assertEquals("POST", post.method());
        assertEquals("https://api.trello.com/1/cards", post.uri().toString());
        assertEquals("OAuth oauth_consumer_key=\"test-key\", oauth_token=\"test-token\"",
                post.headers().firstValue("Authorization").orElseThrow());
        assertEquals(Map.of("idList", LIST_ID, "name", "Follow up",
                "desc", "Quotes: \"hello\"\nUnicode: \u00e9"), objectMapper.readValue(body(post), Map.class));
        assertFalse(text(result).contains("test-token"));
    }

    @Test
    void allowsOmittedDescription() throws Exception {
        respond(openList(), response(200, "{\"id\":\"card-id\",\"url\":\"https://trello.com/c/card\"}"));
        assertFalse(Boolean.TRUE.equals(tool.call(arguments()).isError()));
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("", objectMapper.readTree(body(requests.getAllValues().get(1))).path("desc").asText());
    }

    @Test
    void rejectsInvalidArgumentsBeforeAnyRequest() {
        List<Map<String, Object>> invalid = List.of(
                Map.of(), Map.of("listId", LIST_ID), Map.of("listId", LIST_ID, "name", "Card"),
                Map.of("listId", 42, "name", "Card"),
                Map.of("listId", "../cards", "name", "Card"), Map.of("listId", LIST_ID, "name", "  "),
                Map.of("listId", LIST_ID, "name", 42),
                Map.of("listId", LIST_ID, "name", "x".repeat(16385)),
                Map.of("listId", LIST_ID, "name", "Card", "description", 42),
                Map.of("listId", LIST_ID, "name", "Card", "description", "x".repeat(16385)),
                Map.of("listId", LIST_ID, "name", "Card", "boardId", BOARD_ID));
        for (var args : invalid) {
            assertTrue(text(tool.call(args)).contains("errorCode=INVALID_ARGUMENTS"));
        }
        var nullDescription = new HashMap<String, Object>(arguments());
        nullDescription.put("description", null);
        assertTrue(text(tool.call(nullDescription)).contains("errorCode=INVALID_ARGUMENTS"));
        assertTrue(text(tool.call(null)).contains("errorCode=INVALID_ARGUMENTS"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void missingCredentialsOrInvalidBoardProduceConfigurationError() {
        for (String field : List.of("apiKey", "apiToken", "boardId")) {
            Object original = ReflectionTestUtils.getField(tool, field);
            ReflectionTestUtils.setField(tool, field, "");
            assertTrue(text(tool.call(arguments())).contains("errorCode=CONFIGURATION_ERROR"));
            ReflectionTestUtils.setField(tool, field, original);
        }
        ReflectionTestUtils.setField(tool, "boardId", "short-url-id");
        assertTrue(text(tool.call(arguments())).contains("errorCode=CONFIGURATION_ERROR"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void rejectsOtherBoardsAndClosedColumnsWithoutWriting() throws Exception {
        respond(listOnBoard("cccccccccccccccccccccccc", false),
                listOnBoard(BOARD_ID, true));
        assertTrue(text(tool.call(arguments())).contains("errorCode=TRELLO_ERROR"));
        assertTrue(text(tool.call(arguments())).contains("errorCode=TRELLO_ERROR"));
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.method().equals("GET")));
    }

    @Test
    void listFailuresNeverAttemptCreation(CapturedOutput output) throws Exception {
        respond(response(401, "private-response-test-token"), response(200, "{}"),
                response(200, "not-json-private-response"), response(200, "null"));
        for (int i = 0; i < 4; i++) {
            String result = text(tool.call(arguments()));
            assertTrue(result.contains("errorCode=TRELLO_ERROR"));
            assertTrue(result.contains("No card creation was attempted"));
            assertFalse(result.contains("private-response"));
        }
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(4)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.method().equals("GET")));
        assertFalse(output.getAll().contains("private-response"));
        assertFalse(output.getAll().contains("test-token"));
    }

    @Test
    void failedWriteWarnsAboutDuplicatesAndNeverRetries(CapturedOutput output) throws Exception {
        respond(openList(), response(429, "private-response-test-token"));
        var result = tool.call(arguments());
        assertTrue(Boolean.TRUE.equals(result.isError()));
        assertTrue(text(result).contains("Check the board before retrying"));
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        assertFalse(output.getAll().contains("private-response"));
        assertFalse(output.getAll().contains("test-token"));
    }

    @Test
    void lostWriteResponseWarnsAboutDuplicatesWithoutLeakingException(CapturedOutput output) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    if (((HttpRequest) invocation.getArgument(0)).method().equals("GET")) {
                        return openList();
                    }
                    throw new IOException("private-card-content-test-token");
                });
        assertTrue(text(tool.call(arguments())).contains("Check the board before retrying"));
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        assertFalse(output.getAll().contains("private-card-content"));
    }

    @Test
    void invalidWriteResponseDoesNotClaimSuccess() throws Exception {
        respond(openList(), response(200, "{}"));
        assertTrue(text(tool.call(arguments())).contains("Check the board before retrying"));
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void interruptionPreservesInterruptFlag() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException());
        try {
            assertTrue(text(tool.call(arguments())).contains("errorCode=TRELLO_ERROR"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private Map<String, Object> arguments() {
        return Map.of("name", "Card");
    }

    private HttpResponse<String> openList() {
        return listOnBoard(BOARD_ID, false);
    }

    private HttpResponse<String> listOnBoard(String idBoard, boolean closed) {
        return response(200, "[{\"id\":\"" + LIST_ID + "\",\"name\":\"Feature Requests\","
                + "\"idBoard\":\"" + idBoard + "\",\"closed\":" + closed + "}]");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    @SafeVarargs
    private void respond(HttpResponse<String> first, HttpResponse<String>... rest) throws Exception {
        doReturn(first, (Object[]) rest).when(httpClient)
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    private static String body(HttpRequest request) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var complete = new CompletableFuture<String>();
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
                complete.completeExceptionally(error);
            }

            public void onComplete() {
                complete.complete(bytes.toString(StandardCharsets.UTF_8));
            }
        });
        return complete.get(5, TimeUnit.SECONDS);
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
