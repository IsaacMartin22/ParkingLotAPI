package apiservice.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class McpClientConfig {

    @Bean(initMethod = "initialize", destroyMethod = "closeGracefully")
    McpSyncClient mcpClient(@Value("${app.chat.mcp.base-url}") String baseUrl) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint("/mcp/sse")
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("parking-lot-api", "0.0.3"))
                .requestTimeout(Duration.ofSeconds(20))
                .initializationTimeout(Duration.ofSeconds(20))
                .build();
    }
}
