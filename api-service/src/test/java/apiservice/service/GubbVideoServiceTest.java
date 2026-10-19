package apiservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GubbVideoServiceTest {
    private static final String VIDEO_URL = "https://www.tiktok.com/@example/video/123";

    @TempDir
    Path directory;
    private GubbVideoService videos;
    private Process process;
    private final AtomicLong nanoTime = new AtomicLong();

    @BeforeEach
    void setUp() throws Exception {
        videos = spy(new GubbVideoService(directory.toString(), nanoTime::get));
        process = mock(Process.class);
        when(process.waitFor(5, TimeUnit.MINUTES)).thenReturn(true);
        when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);
        doAnswer(invocation -> {
            Path workspace = invocation.getArgument(0);
            String fileName = invocation.getArgument(1);
            Files.write(workspace.resolve(fileName), new byte[]{1, 2, 3});
            Files.writeString(workspace.resolve("yt-dlp.log"), "download output");
            return process;
        }).when(videos).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));
    }

    @AfterEach
    void tearDown() {
        videos.shutdown();
    }

    @Test
    void jobReturnsWhileDownloadIsStillRunningAndEventuallyPublishesVideo() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(process.waitFor(5, TimeUnit.MINUTES)).thenAnswer(invocation -> {
            started.countDown();
            return release.await(5, TimeUnit.SECONDS);
        });

        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertEquals("processing", videos.getJob(job.id()).status());
            assertThrows(ResponseStatusException.class, () -> videos.getVideo(job.id()));
        } finally {
            release.countDown();
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertEquals("completed", videos.getJob(job.id()).status()));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(videos.getVideo(job.id()).getFile().toPath()));
    }

    @Test
    void failedJobReportsSafeErrorAndDoesNotPublishVideo() throws Exception {
        when(process.exitValue()).thenReturn(1);

        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertEquals("failed", videos.getJob(job.id()).status()));
        assertEquals("Video download failed. Please try again.", videos.getJob(job.id()).error());
        assertThrows(ResponseStatusException.class, () -> videos.getVideo(job.id()));
    }

    @Test
    void rejectsUnknownJobsAndNewJobsAfterShutdown() {
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> videos.getJob(UUID.randomUUID().toString()));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());

        videos.shutdown();

        ResponseStatusException unavailable = assertThrows(ResponseStatusException.class,
                () -> videos.startJob(VIDEO_URL));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
    }

    @Test
    void cancelsRunningDownloadAndStopsItsChildren() throws Exception {
        CountDownLatch started = blockDownload();
        ProcessHandle child = mock(ProcessHandle.class);
        when(process.isAlive()).thenReturn(true);
        when(process.descendants()).thenAnswer(invocation -> Stream.of(child));
        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));

        assertEquals("cancelled", videos.cancelJob(job.id()).status());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(child).destroyForcibly();
            verify(process).destroyForcibly();
            assertStorageEmpty();
        });
        assertEquals("cancelled", videos.getJob(job.id()).status());
        assertEquals("cancelled", videos.cancelJob(job.id()).status());
        assertThrows(ResponseStatusException.class, () -> videos.getVideo(job.id()));
    }

    @Test
    void interruptionAtDownloadCompletionDoesNotPublishVideo() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(process.waitFor(5, TimeUnit.MINUTES)).thenAnswer(invocation -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            // Simulate a process finishing just as an interrupt arrives.
            Thread.currentThread().interrupt();
            return true;
        });
        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            release.countDown();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(
                    () -> assertEquals("failed", videos.getJob(job.id()).status()));
            assertStorageEmpty();
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancelledQueuedJobNeverStartsAndOnlyOneJobRuns() throws Exception {
        CountDownLatch started = blockDownload();
        GubbVideoService.DownloadJob running = videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        GubbVideoService.DownloadJob queued = videos.startJob(VIDEO_URL);
        assertEquals("queued", videos.getJob(queued.id()).status());

        videos.cancelJob(queued.id());
        videos.cancelJob(running.id());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::assertStorageEmpty);
        verify(videos, times(1)).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));
        assertEquals("cancelled", videos.getJob(queued.id()).status());
    }

    @Test
    void fullQueueRejectsNewWorkAndCancellationFreesQueuedSlot() throws Exception {
        CountDownLatch started = blockDownload();
        videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        GubbVideoService.DownloadJob queued = videos.startJob(VIDEO_URL);
        for (int i = 1; i < 20; i++) {
            videos.startJob(VIDEO_URL);
        }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> videos.startJob(VIDEO_URL)).getStatusCode());
        videos.cancelJob(queued.id());
        assertEquals("queued", videos.startJob(VIDEO_URL).status());
        verify(videos, times(1)).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));

        videos.shutdown();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::assertStorageEmpty);
    }

    @Test
    void pollingFailureExpiresRunningAndQueuedJobs() throws Exception {
        CountDownLatch started = blockDownload();
        GubbVideoService.DownloadJob running = videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        GubbVideoService.DownloadJob queued = videos.startJob(VIDEO_URL);

        nanoTime.addAndGet(Duration.ofSeconds(30).toNanos());
        videos.cancelUnpolledJobs();

        assertEquals("cancelled", videos.getJob(running.id()).status());
        assertEquals("cancelled", videos.getJob(queued.id()).status());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::assertStorageEmpty);
        verify(videos, times(1)).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));
    }

    @Test
    void successfulPollRenewsLeaseButLatePollCannotReviveJob() throws Exception {
        CountDownLatch started = blockDownload();
        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));

        nanoTime.addAndGet(Duration.ofSeconds(20).toNanos());
        assertEquals("processing", videos.getJob(job.id()).status());
        nanoTime.addAndGet(Duration.ofSeconds(20).toNanos());
        videos.cancelUnpolledJobs();
        nanoTime.addAndGet(Duration.ofSeconds(10).toNanos());

        assertEquals("cancelled", videos.getJob(job.id()).status());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::assertStorageEmpty);
    }

    @Test
    void shutdownCancelsRunningAndQueuedJobs() throws Exception {
        CountDownLatch started = blockDownload();
        GubbVideoService.DownloadJob running = videos.startJob(VIDEO_URL);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        GubbVideoService.DownloadJob queued = videos.startJob(VIDEO_URL);

        videos.shutdown();

        assertEquals("cancelled", videos.getJob(running.id()).status());
        assertEquals("cancelled", videos.getJob(queued.id()).status());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::assertStorageEmpty);
    }

    @Test
    void cancellationAndLeaseExpiryPreserveCompletedAndFailedJobs() throws Exception {
        GubbVideoService.DownloadJob completed = videos.startJob(VIDEO_URL);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertEquals("completed", videos.getJob(completed.id()).status()));
        when(process.exitValue()).thenReturn(1);
        GubbVideoService.DownloadJob failed = videos.startJob(VIDEO_URL);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertEquals("failed", videos.getJob(failed.id()).status()));

        nanoTime.addAndGet(Duration.ofMinutes(1).toNanos());
        videos.cancelUnpolledJobs();

        assertEquals("completed", videos.cancelJob(completed.id()).status());
        assertEquals("failed", videos.cancelJob(failed.id()).status());
        assertTrue(videos.getVideo(completed.id()).exists());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> videos.cancelJob("missing")).getStatusCode());
    }

    @Test
    void downloadStartupFailureMarksJobFailed() throws Exception {
        doThrow(new IOException("Cannot start downloader"))
                .when(videos).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));

        GubbVideoService.DownloadJob job = videos.startJob(VIDEO_URL);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertEquals("failed", videos.getJob(job.id()).status()));
        assertStorageEmpty();
    }

    @Test
    void publishesCompletedDownloadAndRemovesWorkspace() throws Exception {
        String id = videos.download(VIDEO_URL);

        assertEquals(id, UUID.fromString(id).toString());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(videos.getVideo(id).getFile().toPath()));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(directory.resolve(id + ".mp4")), files.toList());
        }
    }

    @Test
    void multipleDownloadsAreStoredUnderUniqueNames() throws Exception {
        String first = videos.download(VIDEO_URL);
        String second = videos.download(VIDEO_URL);

        assertNotEquals(first, second);
        assertTrue(Files.isRegularFile(directory.resolve(first + ".mp4")));
        assertTrue(Files.isRegularFile(directory.resolve(second + ".mp4")));
        assertEquals(2, videos.listVideos().size());
        verify(videos).startDownload(any(Path.class), eq(first + ".mp4"), eq(VIDEO_URL));
        verify(videos).startDownload(any(Path.class), eq(second + ".mp4"), eq(VIDEO_URL));
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
        doAnswer(invocation -> process).when(videos).startDownload(any(Path.class), anyString(), eq(VIDEO_URL));

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
        verify(process).waitFor(5, TimeUnit.SECONDS);
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

    @Test
    void listsOnlyStoredVideosSortedByExpiry() throws Exception {
        String currentId = UUID.randomUUID().toString();
        Files.write(directory.resolve(currentId + ".mp4"), new byte[]{1, 2, 3});
        String oldId = UUID.randomUUID().toString();
        Path old = Files.writeString(directory.resolve(oldId + ".mp4"), "old");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(Duration.ofDays(3))));
        Files.writeString(directory.resolve("notes.txt"), "ignore");
        Files.createTempDirectory(directory, ".download-");

        var listed = videos.listVideos();

        assertEquals(2, listed.size());
        GubbVideoService.StoredVideo first = listed.get(0);
        assertEquals(oldId, first.id());
        assertTrue(first.expired());
        assertNull(first.jobStatus());
        assertEquals(first.storedAt().plus(Duration.ofDays(2)), first.expiresAt());
        GubbVideoService.StoredVideo second = listed.get(1);
        assertEquals(currentId, second.id());
        assertFalse(second.expired());
        assertEquals(3, second.sizeBytes());
    }

    @Test
    void deletesVideoEarly() throws Exception {
        String id = UUID.randomUUID().toString();
        Files.writeString(directory.resolve(id + ".mp4"), "video");

        videos.deleteVideo(id);

        assertStorageEmpty();
        for (String missing : new String[]{id, "../secret"}) {
            ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                    () -> videos.deleteVideo(missing));
            assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        }
    }

    private void assertStorageEmpty() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }

    private CountDownLatch blockDownload() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        when(process.waitFor(5, TimeUnit.MINUTES)).thenAnswer(invocation -> {
            started.countDown();
            return new CountDownLatch(1).await(5, TimeUnit.SECONDS);
        });
        return started;
    }
}
