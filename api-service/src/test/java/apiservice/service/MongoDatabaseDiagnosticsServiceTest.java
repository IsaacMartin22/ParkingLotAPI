package apiservice.service;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoDatabaseDiagnosticsServiceTest {

    @Test
    void returnsMongoServerAndDatabaseMetrics() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.executeCommand(any(Document.class))).thenAnswer(invocation -> {
            Document command = invocation.getArgument(0);
            if (command.containsKey("ping")) {
                return new Document("ok", 1);
            }
            if (command.containsKey("serverStatus")) {
                return new Document("uptimeMillis", 120_000L)
                        .append("connections", new Document("current", 4).append("available", 96));
            }
            if (command.containsKey("dbStats")) {
                return new Document("totalSize", 4_096L);
            }
            if (command.containsKey("currentOp")) {
                return new Document("inprog", List.of(
                        new Document("active", true)
                                .append("secs_running", 6)
                                .append("command", new Document("find", "portfolio_documents")),
                        new Document("active", true)
                                .append("secs_running", 5)
                                .append("command", new Document("find", "recent_documents"))
                ));
            }
            return new Document();
        });

        var response = new MongoDatabaseDiagnosticsService(mongoTemplate).getDiagnostics();

        assertTrue(response.connectivity());
        assertTrue(response.latency() >= 0);
        assertEquals(120_000L, response.uptimeMillis());
        assertEquals(4L, response.activeConnections());
        assertEquals(100L, response.maxConnections());
        assertEquals(4_096L, response.databaseSize());
        assertEquals(1, response.longRunningOperations().size());
        assertEquals(6_000L, response.longRunningOperations().get(0).timeRunningMillis());
        assertTrue(response.longRunningOperations().get(0).queryText().contains("find"));
    }
}
