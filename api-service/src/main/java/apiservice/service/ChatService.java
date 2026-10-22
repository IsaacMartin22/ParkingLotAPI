package apiservice.service;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public interface ChatService {
    List<McpSchema.Tool> getAvailableTools();

    String ask(String question);

    String ask(String question, String model);

    ChatAnswer askWithCitation(String question);

    ChatAnswer askWithCitation(String question, String model);
}
