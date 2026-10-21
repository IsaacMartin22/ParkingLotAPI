package parkinglot.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfiguration {

    @Bean
    HttpServletSseServerTransportProvider mcpTransport(ObjectMapper objectMapper) {
        return new HttpServletSseServerTransportProvider(objectMapper, "/mcp/messages", "/mcp/sse");
    }

    @Bean
    ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServlet(
            HttpServletSseServerTransportProvider transport,
            McpSyncServer server
    ) {
        ServletRegistrationBean<HttpServletSseServerTransportProvider> registration =
                new ServletRegistrationBean<>(transport, "/mcp/sse", "/mcp/messages");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean(destroyMethod = "closeGracefully")
    McpSyncServer mcpServer(
            HttpServletSseServerTransportProvider transport, SiteAnalyticsTool siteAnalyticsTool,
            PortfolioSearchTool portfolioSearchTool, TrelloCreateCardTool trelloCreateCardTool
    ) {
        return McpServer.sync(transport)
                .serverInfo("parking-lot-mcp-server", "0.0.2")
                .tools(tools(siteAnalyticsTool, portfolioSearchTool, trelloCreateCardTool))
                .build();
    }

    static List<McpServerFeatures.SyncToolSpecification> tools(
            SiteAnalyticsTool siteAnalyticsTool, PortfolioSearchTool portfolioSearchTool,
            TrelloCreateCardTool trelloCreateCardTool
    ) {
        return List.of(siteAnalyticsTool.specification(), portfolioSearchTool.specification(),
                trelloCreateCardTool.specification());
    }
}
