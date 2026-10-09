package apiservice.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationsTest {

    private static final Path MIGRATIONS_DIR = Path.of("src", "main", "resources", "db", "migration");
    private static final Pattern MIGRATION_FILE = Pattern.compile("^V(\\d+)__.+\\.sql$");
    private static final String V7_SHA256 = "e9146b4eef6499118535137169db6525f3850d2c4850d34fa1e9c45277658559";
    private static final String V8_SHA256 = "dff1e8bca931ccf188adf7be8bdd7ea6034ad294fc13b1d9567dfabf913f67b9";

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
    void appliedMigrationsShouldNotBeEdited() throws IOException, NoSuchAlgorithmException {
        assertKnownChecksum("V7__create_chat_interactions.sql", V7_SHA256);
        assertKnownChecksum("V8__add_chat_interaction_metrics.sql", V8_SHA256);
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

    private static void assertKnownChecksum(String fileName, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
        Path migration = MIGRATIONS_DIR.resolve(fileName);
        assertTrue(Files.exists(migration), "Missing migration file: " + fileName);

        String actualSha256 = sha256(Files.readAllBytes(migration));
        assertEquals(expectedSha256, actualSha256,
                "Applied migration was edited: " + fileName + ". Add a new migration instead.");
    }

    private static String sha256(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content));
    }
}
