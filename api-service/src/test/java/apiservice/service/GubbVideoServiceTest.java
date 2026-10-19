package apiservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GubbVideoServiceTest {
    private static final String VIDEO_URL = "https://www.tiktok.com/@example/video/123";

    @TempDir
    Path directory;
    private GubbVideoService videos;
    private Process process;

    @BeforeEach
    void setUp() throws Exception {
        videos = spy(new GubbVideoService(directory.toString()));
        process = mock(Process.class);
        when(process.waitFor(5, TimeUnit.MINUTES)).thenReturn(true);
        doAnswer(invocation -> {
            Path workspace = invocation.getArgument(0);
            Files.write(workspace.resolve("video.mp4"), new byte[]{1, 2, 3});
            Files.writeString(workspace.resolve("yt-dlp.log"), "download output");
            return process;
        }).when(videos).startDownload(any(Path.class), eq(VIDEO_URL));
    }

    @Test
    void publishesCompletedDownloadAndRemovesWorkspace() throws Exception {
        String id = videos.download(VIDEO_URL);

        assertEquals(id, UUID.fromString(id).toString());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(videos.getVideo(id).getFile().toPath()));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void failedDownloadIsNotPublishedAndIncludesProcessOutput() throws Exception {
        when(process.exitValue()).thenReturn(1);

        IOException failure = assertThrows(IOException.class, () -> videos.download(VIDEO_URL));

        assertTrue(failure.getMessage().contains("download output"));
        assertStorageEmpty();
    }

    @Test
    void rejectsSuccessfulProcessWithoutVideo() throws Exception {
        doAnswer(invocation -> process).when(videos).startDownload(any(Path.class), eq(VIDEO_URL));

        assertThrows(IOException.class, () -> videos.download(VIDEO_URL));
        assertStorageEmpty();
    }

    @Test
    void timeoutStopsProcessAndRemovesPartialDownload() throws Exception {
        when(process.waitFor(5, TimeUnit.MINUTES)).thenReturn(false);
        when(process.isAlive()).thenReturn(true);
        when(process.descendants()).thenReturn(Stream.empty());

        assertThrows(IOException.class, () -> videos.download(VIDEO_URL));

        verify(process).destroyForcibly();
        verify(process).waitFor();
        assertStorageEmpty();
    }

    @Test
    void interruptionStopsProcessAndRemovesPartialDownload() throws Exception {
        when(process.waitFor(5, TimeUnit.MINUTES)).thenThrow(new InterruptedException());
        when(process.isAlive()).thenReturn(true);
        when(process.descendants()).thenReturn(Stream.empty());

        assertThrows(InterruptedException.class, () -> videos.download(VIDEO_URL));

        verify(process).destroyForcibly();
        assertStorageEmpty();
    }

    @Test
    void rejectsExpiredMissingAndTraversalPaths() throws Exception {
        String expiredId = UUID.randomUUID().toString();
        Path expired = Files.writeString(directory.resolve(expiredId + ".mp4"), "old");
        Files.setLastModifiedTime(expired, FileTime.from(Instant.now().minus(Duration.ofDays(8))));

        for (String id : new String[]{expiredId, UUID.randomUUID().toString(), "../secret"}) {
            ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                    () -> videos.getVideo(id));
            assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        }
    }

    @Test
    void cleanupRemovesOnlyExpiredDownloadsAndAbandonedWorkspaces() throws Exception {
        Path expired = Files.writeString(directory.resolve(UUID.randomUUID() + ".mp4"), "old");
        Path current = Files.writeString(directory.resolve(UUID.randomUUID() + ".mp4"), "new");
        Path unrelated = Files.writeString(directory.resolve("notes.txt"), "keep");
        Path abandoned = Files.createTempDirectory(directory, ".download-");
        Files.writeString(abandoned.resolve("video.mp4.part"), "partial");
        Path active = Files.createTempDirectory(directory, ".download-");
        FileTime old = FileTime.from(Instant.now().minus(Duration.ofDays(8)));
        Files.setLastModifiedTime(expired, old);
        Files.setLastModifiedTime(unrelated, old);
        Files.setLastModifiedTime(abandoned, old);

        videos.deleteExpiredVideos();

        assertFalse(Files.exists(expired));
        assertFalse(Files.exists(abandoned));
        assertTrue(Files.exists(current));
        assertTrue(Files.exists(unrelated));
        assertTrue(Files.exists(active));
    }

    private void assertStorageEmpty() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }
}
