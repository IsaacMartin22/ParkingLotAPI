package apiservice.controller;

import apiservice.service.GubbVideoService;
import apiservice.service.GubbVideoService.DownloadJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GubbControllerTest {
    private static final String TIKTOK_URL =
            "https://www.tiktok.com/@hunainn/video/7696938907090439444";
    private final GubbVideoService videos = mock(GubbVideoService.class);
    private MockMvc mvc;

    @TempDir
    Path directory;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new GubbController(videos)).build();
    }

    @Test
    void returnsJobImmediatelyWithoutWaitingForDownload() throws Exception {
        when(videos.startJob(TIKTOK_URL)).thenReturn(
                new DownloadJob("video-id", "queued", null, Instant.now()));

        mvc.perform(get("https://api.example.com/api/gubb").secure(true)
                        .with(request -> {
                            request.setServerName("api.example.com");
                            request.setServerPort(443);
                            return request;
                        }).param("url", TIKTOK_URL))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://api.example.com/api/gubb/jobs/video-id"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.jobId").value("video-id"))
                .andExpect(jsonPath("$.statusUrl").value("https://api.example.com/api/gubb/jobs/video-id"))
                .andExpect(jsonPath("$.url").doesNotExist());
        verify(videos).startJob(TIKTOK_URL);
        verifyNoMoreInteractions(videos);
    }

    @Test
    void pollsUntilVideoIsReady() throws Exception {
        when(videos.getJob("video-id")).thenReturn(
                new DownloadJob("video-id", "processing", null, Instant.now()),
                new DownloadJob("video-id", "completed", null, Instant.now()));

        mvc.perform(get("/api/gubb/jobs/video-id"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.url").doesNotExist());

        mvc.perform(get("/api/gubb/jobs/video-id"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.url").value("http://localhost/api/gubb/videos/video-id"));
    }

    @Test
    void reportsFailedJobSoClientCanStopPolling() throws Exception {
        when(videos.getJob("video-id")).thenReturn(
                new DownloadJob("video-id", "failed", "Video download failed. Please try again.", Instant.now()));

        mvc.perform(get("/api/gubb/jobs/video-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("Video download failed. Please try again."))
                .andExpect(jsonPath("$.url").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownJob() throws Exception {
        when(videos.getJob("missing")).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Download job not found or expired"));

        mvc.perform(get("/api/gubb/jobs/missing")).andExpect(status().isNotFound());
    }

    @Test
    void returnsServiceUnavailableWhenQueueIsFull() throws Exception {
        when(videos.startJob(TIKTOK_URL)).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Download queue is full"));

        mvc.perform(get("/api/gubb").param("url", TIKTOK_URL))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void streamsStoredVideoAndSupportsSeeking() throws Exception {
        Path video = Files.write(directory.resolve("video.mp4"), new byte[]{0, 1, 2, 3, 4, 5});
        when(videos.getVideo("video-id")).thenReturn(new FileSystemResource(video));

        mvc.perform(get("/api/gubb/videos/video-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(new byte[]{0, 1, 2, 3, 4, 5}))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        mvc.perform(get("/api/gubb/videos/video-id").header(HttpHeaders.RANGE, "bytes=2-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-4/6"))
                .andExpect(content().bytes(new byte[]{2, 3, 4}));

        mvc.perform(get("/api/gubb/videos/video-id").header(HttpHeaders.RANGE, "bytes=20-30"))
                .andExpect(status().isRequestedRangeNotSatisfiable());
    }

    @Test
    void returnsNotFoundForUnavailableVideo() throws Exception {
        when(videos.getVideo("missing")).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found or expired"));
        mvc.perform(get("/api/gubb/videos/missing")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidDownloadRequestBeforeStartingProcess() throws Exception {
        mvc.perform(get("/api/gubb").param("url", "https://example.com/video/123"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(videos);
    }

    @Test
    void acceptsSampleTikTokVideoUrl() {
        assertDoesNotThrow(() -> GubbController.validateTikTokUrl(TIKTOK_URL));
    }

    @Test
    void rejectsUrlsOutsideTikTokHosts() {
        assertThrows(IllegalArgumentException.class,
                () -> GubbController.validateTikTokUrl("https://example.com/video/123"));
    }
}
