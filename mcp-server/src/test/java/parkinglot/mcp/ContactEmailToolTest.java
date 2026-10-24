//package parkinglot.mcp;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.modelcontextprotocol.spec.McpSchema;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.Flow;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoInteractions;
//import static org.mockito.Mockito.when;
//
//class ContactEmailToolTest {
//
//    private final HttpClient httpClient = mock(HttpClient.class);
//    private final ContactEmailTool tool = new ContactEmailTool(new ObjectMapper());
//
//    @BeforeEach
//    void configure() {
//        ReflectionTestUtils.setField(tool, "httpClient", httpClient);
//        ReflectionTestUtils.setField(tool, "contactApiUrl", "http://localhost:8080/api/contact");
//    }
//
//    @Test
//    void sendsSubjectAndMessageToContactService() throws Exception {
//        HttpResponse<Void> response = mock(HttpResponse.class);
//        when(response.statusCode()).thenReturn(202);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
//
//        McpSchema.CallToolResult result = tool.call(Map.of("subject", "Hello", "message", "Please contact me."));
//
//        assertFalse(Boolean.TRUE.equals(result.isError()));
//        var request = ArgumentCaptor.forClass(HttpRequest.class);
//        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
//        assertEquals("POST", request.getValue().method());
//        assertEquals("http://localhost:8080/api/contact", request.getValue().uri().toString());
//        assertEquals(Map.of("subject", "Hello", "message", "Please contact me."),
//                new ObjectMapper().readValue(body(request.getValue()), Map.class));
//    }
//
//    @Test
//    void rejectsInvalidArgumentsWithoutSendingRequest() {
//        for (Map<?, ?> arguments : List.of(
//                Map.of(), Map.of("subject", "Hello"), Map.of("subject", " ", "message", "Body"),
//                Map.of("subject", "Hello", "message", "Body", "to", "other@example.com")
//        )) {
//            assertTrue(Boolean.TRUE.equals(tool.call((Map<String, Object>) arguments).isError()));
//        }
//        assertTrue(Boolean.TRUE.equals(tool.call(null).isError()));
//        verifyNoInteractions(httpClient);
//    }
//
//    @Test
//    void reportsContactValidationFailureWithStatusAndDiagnostic() throws Exception {
//        HttpResponse<Void> response = mock(HttpResponse.class);
//        when(response.statusCode()).thenReturn(400);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
//
//        McpSchema.CallToolResult result = tool.call(Map.of("subject", "Hello", "message", "Hello\nIsaac"));
//
//        assertTrue(Boolean.TRUE.equals(result.isError()));
//        assertTrue(text(result).contains("[MCP errorCode=CONTACT_ERROR httpStatus=400 errorId="));
//        assertTrue(text(result).contains("disallows line breaks"));
//        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    void doesNotRetryAnUncertainRequest() throws Exception {
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenThrow(new IOException("connection lost"));
//
//        McpSchema.CallToolResult result = tool.call(Map.of("subject", "Hello", "message", "Body"));
//
//        assertTrue(Boolean.TRUE.equals(result.isError()));
//        assertTrue(text(result).contains("Check before retrying"));
//        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//    }
//
//    private static String body(HttpRequest request) throws Exception {
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        CompletableFuture<String> complete = new CompletableFuture<>();
//        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
//            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
//                subscription.request(Long.MAX_VALUE);
//            }
//
//            public void onNext(ByteBuffer buffer) {
//                byte[] bytes = new byte[buffer.remaining()];
//                buffer.get(bytes);
//                output.writeBytes(bytes);
//            }
//
//            public void onError(Throwable error) {
//                complete.completeExceptionally(error);
//            }
//
//            public void onComplete() {
//                complete.complete(output.toString(StandardCharsets.UTF_8));
//            }
//        });
//        return complete.get(5, TimeUnit.SECONDS);
//    }
//
//    private static String text(McpSchema.CallToolResult result) {
//        return ((McpSchema.TextContent) result.content().get(0)).text();
//    }
//}
