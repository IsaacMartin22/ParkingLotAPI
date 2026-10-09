package apiservice.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    void latestMigrationShouldIncreaseEmbeddingDimension() throws IOException {
        Path latestMigration = Files.list(MIGRATIONS_DIR)
                .filter(Files::isRegularFile)
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .orElseThrow();

        String sql = Files.readString(latestMigration);

        assertTrue(sql.contains("vector(3072)"),
                "Latest migration should include the current embedding dimension");
    }
}
