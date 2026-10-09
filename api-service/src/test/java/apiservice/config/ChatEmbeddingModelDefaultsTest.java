package apiservice.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEmbeddingModelDefaultsTest {

    private static final Path APPLICATION_PROPERTIES = Path.of("src", "main", "resources", "application.properties");
    private static final Path CHAT_SERVICE = Path.of("src", "main", "java", "apiservice", "service", "ChatServiceImpl.java");

    @Test
    void embeddingModelDefaultsShouldStayAligned() throws IOException {
        String properties = Files.readString(APPLICATION_PROPERTIES);
        String chatService = Files.readString(CHAT_SERVICE);

        assertTrue(properties.contains("app.chat.openai-embedding-model=${OPENAI_EMBEDDING_MODEL:text-embedding-3-large}"));
        assertTrue(chatService.contains("@Value(\"${app.chat.openai-embedding-model:text-embedding-3-large}\")"));
    }
}
