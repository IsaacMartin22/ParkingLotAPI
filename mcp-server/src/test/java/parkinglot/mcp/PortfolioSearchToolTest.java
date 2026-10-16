package parkinglot.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import io.modelcontextprotocol.spec.McpSchema;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PortfolioSearchToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final HttpClient httpClient = mock(HttpClient.class);

    @Test
    @SuppressWarnings("unchecked")
    void preservesVectorSearchAndReturnsTextMetadataAndScoresWithoutEmbeddings() throws Exception {
        PortfolioSearchTool tool = tool();
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoTemplate.getCollection("portfolio_documents")).thenReturn(collection);
        AggregateIterable<Document> vectorResults = results(List.of(
                new Document("_id", "chunk-1").append("text", "Isaac builds Java APIs.")
                        .append("metadata", Map.of("citation", "https://example.com/resume"))
                        .append("score", 0.9)
        ));
        when(collection.aggregate(anyList())).thenReturn(vectorResults);

        McpSchema.CallToolResult result = tool.call(Map.of("query", " Isaac's experience "));

        assertFalse(Boolean.TRUE.equals(result.isError()), text(result));
        JsonNode document = objectMapper.readTree(text(result)).path("documents").path(0);
        assertEquals("chunk-1", document.path("id").asText());
        assertEquals("Isaac builds Java APIs.", document.path("text").asText());
        assertEquals("https://example.com/resume", document.path("metadata").path("citation").asText());
        assertEquals(0.9, document.path("score").asDouble());
        assertFalse(document.has("embedding"));
        ArgumentCaptor<List<Document>> pipelines = ArgumentCaptor.forClass(List.class);
        verify(collection).aggregate(pipelines.capture());
        Document search = pipelines.getValue().get(0).get("$vectorSearch", Document.class);
        assertEquals("portfolio_documents_vector_index", search.getString("index"));
        assertEquals("embedding", search.getString("path"));
        assertEquals(List.of(0.5, -1.0), search.get("queryVector"));
        assertEquals(50, search.getInteger("numCandidates"));
        assertEquals(5, search.getInteger("limit"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fallsBackToLiteralCaseInsensitiveTextSearchOnlyForEmptyVectorResults() throws Exception {
        PortfolioSearchTool tool = tool();
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoTemplate.getCollection("portfolio_documents")).thenReturn(collection);
        AggregateIterable<Document> emptyResults = results(List.of());
        AggregateIterable<Document> textResults = results(List.of(new Document("text", "C++ experience")));
        when(collection.aggregate(anyList())).thenReturn(emptyResults).thenReturn(textResults);

        McpSchema.CallToolResult result = tool.call(Map.of("query", "C++"));

        assertFalse(Boolean.TRUE.equals(result.isError()), text(result));
        JsonNode document = objectMapper.readTree(text(result)).path("documents").path(0);
        assertEquals("C++ experience", document.path("text").asText());
        assertTrue(document.path("score").isNull());
        ArgumentCaptor<List<Document>> pipelines = ArgumentCaptor.forClass(List.class);
        verify(collection, times(2)).aggregate(pipelines.capture());
        List<Document> fallback = pipelines.getAllValues().get(1);
        Document expression = fallback.get(0).get("$match", Document.class).get("text", Document.class);
        assertEquals(Pattern.quote("C++"), expression.getString("$regex"));
        assertEquals("i", expression.getString("$options"));
        assertEquals(5, fallback.get(1).getInteger("$limit"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyDocumentsWhenNeitherSearchMatches() throws Exception {
        PortfolioSearchTool tool = tool();
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoTemplate.getCollection("portfolio_documents")).thenReturn(collection);
        when(collection.aggregate(anyList())).thenAnswer(invocation -> results(List.of()));

        McpSchema.CallToolResult result = tool.call(Map.of("query", "Unknown topic"));

        assertFalse(Boolean.TRUE.equals(result.isError()), text(result));
        assertEquals(0, objectMapper.readTree(text(result)).path("documents").size());
        verify(collection, times(2)).aggregate(anyList());
    }

    @Test
    void rejectsInvalidArgumentsWithoutCallingOpenAiOrMongo() {
        PortfolioSearchTool tool = new PortfolioSearchTool(mongoTemplate, objectMapper);
        ReflectionTestUtils.setField(tool, "httpClient", httpClient);
        List<Map<String, Object>> arguments = List.of(
                Map.of(), Map.of("query", ""), Map.of("query", "  "),
                Map.of("query", 42), Map.of("query", "Isaac", "limit", 2));
        for (Map<String, Object> args : arguments) {
            assertTrue(Boolean.TRUE.equals(tool.call(args).isError()));
        }
        assertTrue(text(tool.call(null)).contains("errorCode=INVALID_ARGUMENTS"));
        verifyNoInteractions(httpClient, mongoTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsMongoFailureWithoutExposingDatabaseDetailsOrRunningTextFallback() throws Exception {
        PortfolioSearchTool tool = tool();
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoTemplate.getCollection("portfolio_documents")).thenReturn(collection);
        when(collection.aggregate(anyList())).thenThrow(new MongoException("private-database-details"));

        McpSchema.CallToolResult result = tool.call(Map.of("query", "Experience"));

        assertTrue(Boolean.TRUE.equals(result.isError()));
        assertTrue(text(result).contains("errorCode=INTERNAL_ERROR"));
        assertFalse(text(result).contains("private-database-details"));
        verify(collection).aggregate(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsEmbeddingFailureWithoutSearchingMongo() throws Exception {
        PortfolioSearchTool tool = tool();
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        McpSchema.CallToolResult result = tool.call(Map.of("query", "Experience"));

        assertTrue(Boolean.TRUE.equals(result.isError()));
        assertTrue(text(result).contains("errorCode=INTERNAL_ERROR"));
        verifyNoInteractions(mongoTemplate);
    }

    @SuppressWarnings("unchecked")
    private PortfolioSearchTool tool() throws Exception {
        PortfolioSearchTool tool = new PortfolioSearchTool(mongoTemplate, objectMapper);
        ReflectionTestUtils.setField(tool, "httpClient", httpClient);
        ReflectionTestUtils.setField(tool, "openAiApiKey", "test-key");
        ReflectionTestUtils.setField(tool, "embeddingModel", "text-embedding-3-large");
        ReflectionTestUtils.setField(tool, "collectionName", "portfolio_documents");
        ReflectionTestUtils.setField(tool, "vectorIndex", "portfolio_documents_vector_index");
        ReflectionTestUtils.setField(tool, "maxContextChunks", 5);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":[{\"embedding\":[0.5,-1.0]}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return tool;
    }

    @SuppressWarnings("unchecked")
    private AggregateIterable<Document> results(List<Document> documents) {
        AggregateIterable<Document> iterable = mock(AggregateIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        var iterator = documents.iterator();
        when(iterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        when(cursor.next()).thenAnswer(invocation -> iterator.next());
        return iterable;
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
