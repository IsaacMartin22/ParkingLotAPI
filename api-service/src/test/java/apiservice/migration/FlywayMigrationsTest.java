package apiservice.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationsTest {

    private static final Path MIGRATIONS_DIR = Path.of("src", "main", "resources", "db", "migration");
    private static final Pattern MIGRATION_FILE = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @Test
    void migrationsShouldBeVersionedSequentially() throws IOException {
        List<Integer> versions = Files.list(MIGRATIONS_DIR)
                .map(Path::getFileName)
                .map(Path::toString)
                .map(MIGRATION_FILE::matcher)
                .filter(Matcher::matches)
                .map(matcher -> Integer.parseInt(matcher.group(1)))
                .sorted()
                .toList();

        assertFalse(versions.isEmpty(), "Expected Flyway migrations to exist");

        for (int i = 0; i < versions.size(); i++) {
            assertEquals(i + 1, versions.get(i), "Migration versions must be sequential with no gaps");
        }
    }

    @Test
    void baseChatMigrationShouldKeepOriginalEmbeddingDimension() throws IOException {
        String sql = Files.readString(MIGRATIONS_DIR.resolve("V7__create_chat_interactions.sql"));

        assertTrue(sql.contains("embedding vector(1536) NOT NULL"),
                "The original chat migration should stay at 1536. Increase dimensions in a new migration.");
    }

    @Test
    void v9ShouldRebuildChatEmbeddingColumnFor3072Dimensions() throws IOException {
        String sql = Files.readString(MIGRATIONS_DIR.resolve("V9__rebuild_chat_embedding_column_for_3072.sql"));

        assertTrue(sql.contains("DROP COLUMN embedding"),
                "V9 should drop the incompatible 1536-dimension embedding column.");
        assertTrue(sql.contains("ADD COLUMN embedding vector(3072)"),
                "V9 should recreate the embedding column with 3072 dimensions.");
    }
}
