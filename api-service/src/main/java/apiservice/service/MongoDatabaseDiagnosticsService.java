package apiservice.service;

import com.mongodb.MongoException;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import parkinglot.common.model.LongRunningQuery;
import parkinglot.common.response.MongoDatabaseDiagnosticsResponse;

import java.util.ArrayList;
import java.util.List;

@Service
public class MongoDatabaseDiagnosticsService {

    private static final long LONG_RUNNING_OPERATION_THRESHOLD_SECONDS = 5L;
    private static final long UNAVAILABLE_METRIC = -1L;

    private final MongoTemplate mongoTemplate;

    public MongoDatabaseDiagnosticsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MongoDatabaseDiagnosticsResponse getDiagnostics() {
        boolean connectivity = checkConnectivity();
        long latency = measureLatency();
        Document serverStatus = queryServerStatus();
        Document databaseStats = queryDatabaseStats();

        return new MongoDatabaseDiagnosticsResponse(
                connectivity,
                latency,
                getLong(serverStatus, "uptimeMillis"),
                getNestedLong(serverStatus, "connections", "current"),
                getMaxConnections(serverStatus),
                getLong(databaseStats, "totalSize"),
                queryLongRunningOperations()
        );
    }

    private boolean checkConnectivity() {
        try {
            executeCommand(new Document("ping", 1));
            return true;
        } catch (MongoException | DataAccessException exception) {
            return false;
        }
    }

    private long measureLatency() {
        try {
            long start = System.currentTimeMillis();
            executeCommand(new Document("ping", 1));
            return System.currentTimeMillis() - start;
        } catch (MongoException | DataAccessException exception) {
            return UNAVAILABLE_METRIC;
        }
    }

    private Document queryServerStatus() {
        try {
            return executeCommand(new Document("serverStatus", 1));
        } catch (MongoException | DataAccessException exception) {
            return new Document();
        }
    }

    private Document queryDatabaseStats() {
        try {
            return executeCommand(new Document("dbStats", 1));
        } catch (MongoException | DataAccessException exception) {
            return new Document();
        }
    }

    private long getMaxConnections(Document serverStatus) {
        long currentConnections = getNestedLong(serverStatus, "connections", "current");
        long availableConnections = getNestedLong(serverStatus, "connections", "available");
        if (currentConnections == UNAVAILABLE_METRIC || availableConnections == UNAVAILABLE_METRIC) {
            return UNAVAILABLE_METRIC;
        }
        return currentConnections + availableConnections;
    }

    private long getLong(Document document, String field) {
        Object value = document.get(field);
        return value instanceof Number number ? number.longValue() : UNAVAILABLE_METRIC;
    }

    private long getNestedLong(Document document, String parentField, String field) {
        Object parent = document.get(parentField);
        return parent instanceof Document nestedDocument
                ? getLong(nestedDocument, field)
                : UNAVAILABLE_METRIC;
    }

    private List<LongRunningQuery> queryLongRunningOperations() {
        try {
            Document result = executeCommand(new Document("currentOp", 1).append("$all", true));
            Object operations = result.get("inprog");
            if (!(operations instanceof List<?> operationList)) {
                return List.of();
            }

            List<LongRunningQuery> longRunningOperations = new ArrayList<>();
            for (Object operation : operationList) {
                if (!(operation instanceof Document operationDocument)
                        || !Boolean.TRUE.equals(operationDocument.getBoolean("active"))) {
                    continue;
                }

                long secondsRunning = getLong(operationDocument, "secs_running");
                if (secondsRunning > LONG_RUNNING_OPERATION_THRESHOLD_SECONDS) {
                    longRunningOperations.add(new LongRunningQuery(
                            secondsRunning * 1_000L,
                            getOperationText(operationDocument)
                    ));
                }
            }
            return longRunningOperations;
        } catch (MongoException | DataAccessException exception) {
            return List.of();
        }
    }

    private String getOperationText(Document operation) {
        Object command = operation.get("command");
        if (command instanceof Document commandDocument) {
            return commandDocument.toJson();
        }
        if (command != null) {
            return command.toString();
        }

        Object operationType = operation.get("op");
        Object namespace = operation.get("ns");
        return (operationType == null ? "unknown" : operationType)
                + (namespace == null ? "" : " " + namespace);
    }

    private Document executeCommand(Document command) {
        return mongoTemplate.executeCommand(command);
    }
}
