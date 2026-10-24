//package parkinglot.mcp;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.modelcontextprotocol.server.McpServerFeatures;
//import io.modelcontextprotocol.spec.McpSchema;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//

// DigitalOcean explicitly blocks port 587 to prevent abuse/spam, so this tool doesn't work. I don't like the workarounds so I just
// won't have this be a tool

//@Component
//public class ContactEmailTool {
//
//    static final String NAME = "send_contact_email";
//    private static final Logger logger = LoggerFactory.getLogger(ContactEmailTool.class);
//    private static final Set<String> ARGUMENTS = Set.of("message", "subject");
//
//    private final ObjectMapper objectMapper;
//    private final HttpClient httpClient = HttpClient.newBuilder()
//            .connectTimeout(Duration.ofSeconds(5)).build();
//
//    @Value("${app.contact.api-url:https://api.isaaccmart.in/api/contact}")
//    private String contactApiUrl;
//
//    public ContactEmailTool(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//    McpServerFeatures.SyncToolSpecification specification() {
//        return new McpServerFeatures.SyncToolSpecification(new McpSchema.Tool(
//                NAME,
//                "Send an email to Isaac through the configured contact service. Use only when the user explicitly "
//                        + "asks to email Isaac. Supply a plain-text subject and message without line breaks, HTML, "
//                        + "or email header lines. Do not retry automatically "
//                        + "after an uncertain outcome.",
//                """
//                {
//                  "type": "object",
//                  "properties": {
//                    "subject": {
//                      "type": "string",
//                      "minLength": 1,
//                      "maxLength": 200,
//                      "description": "Email subject."
//                    },
//                    "message": {
//                      "type": "string",
//                      "minLength": 1,
//                      "maxLength": 4000,
//                      "description": "Email body."
//                    }
//                  },
//                  "required": ["subject", "message"],
//                  "additionalProperties": false
//                }
//                """
//        ), (exchange, arguments) -> call(arguments));
//    }
//
//    McpSchema.CallToolResult call(Map<String, Object> arguments) {
//        String errorId = UUID.randomUUID().toString();
//        long startedAt = System.nanoTime();
//        if (arguments == null || arguments.size() != ARGUMENTS.size()
//                || !ARGUMENTS.containsAll(arguments.keySet())
//                || !(arguments.get("message") instanceof String message) || message.isBlank()
//                || !(arguments.get("subject") instanceof String subject) || subject.isBlank()) {
//            return error("Invalid arguments: supply a non-blank subject and message only.",
//                    "INVALID_ARGUMENTS", errorId);
//        }
//
//        try {
//            String body = objectMapper.writeValueAsString(Map.of("message", message, "subject", subject));
//            HttpRequest request = HttpRequest.newBuilder(URI.create(contactApiUrl))
//                    .timeout(Duration.ofSeconds(15))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(body))
//                    .build();
//            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
//            if (response.statusCode() < 200 || response.statusCode() >= 300) {
//                logger.warn("Contact service rejected MCP email request. tool={}, errorId={}, httpStatus={}",
//                        NAME, errorId, response.statusCode());
//                return new McpSchema.CallToolResult(
//                        "Contact service returned HTTP " + response.statusCode()
//                                + (response.statusCode() == 400
//                                ? ". Supply a subject up to 200 characters and a message up to 4000 characters; "
//                                        + "ContactService disallows line breaks, HTML, and email header patterns."
//                                : ". Email delivery could not be confirmed. Check before retrying.")
//                                + " [MCP errorCode=CONTACT_ERROR httpStatus=" + response.statusCode()
//                                + " errorId=" + errorId + "]", true);
//            }
//            logger.info("MCP tool call completed. tool={}, errorId={}, durationMs={}",
//                    NAME, errorId, (System.nanoTime() - startedAt) / 1_000_000);
//            return new McpSchema.CallToolResult("Email sent to Isaac.", false);
//        } catch (InterruptedException ex) {
//            Thread.currentThread().interrupt();
//            return failure(errorId, startedAt, ex);
//        } catch (IOException | IllegalArgumentException ex) {
//            return failure(errorId, startedAt, ex);
//        }
//    }
//
//    private McpSchema.CallToolResult failure(String errorId, long startedAt, Exception exception) {
//        logger.error("MCP tool call failed. tool={}, errorCode=CONTACT_ERROR, errorId={}, exceptionType={}, durationMs={}",
//                NAME, errorId, exception.getClass().getSimpleName(), (System.nanoTime() - startedAt) / 1_000_000);
//        return error("Email delivery could not be confirmed. Check before retrying.",
//                "CONTACT_ERROR", errorId);
//    }
//
//    private static McpSchema.CallToolResult error(String message, String code, String errorId) {
//        return new McpSchema.CallToolResult(message + " [MCP errorCode=" + code + " errorId=" + errorId + "]", true);
//    }
//}
