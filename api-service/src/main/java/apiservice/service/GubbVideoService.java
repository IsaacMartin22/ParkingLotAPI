package apiservice.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class GubbVideoService {
    private static final Logger log = LoggerFactory.getLogger(GubbVideoService.class);
    private static final Duration RETENTION = Duration.ofDays(7);
    private final Path storageDirectory;
    private final ConcurrentMap<String, DownloadJob> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor downloads = new ThreadPoolExecutor(
            2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));

    public record DownloadJob(String id, String status, String error, Instant updatedAt) {
    }

    public GubbVideoService(@Value("${app.gubb.storage-directory}") String storageDirectory)
            throws IOException {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.storageDirectory);
    }

    public String download(String videoUrl) throws IOException, InterruptedException {
        return download(UUID.randomUUID().toString(), videoUrl);
    }

    public DownloadJob startJob(String videoUrl) {
        String id = UUID.randomUUID().toString();
        DownloadJob job = new DownloadJob(id, "queued", null, Instant.now());
        jobs.put(id, job);
        try {
            downloads.execute(() -> {
                jobs.put(id, new DownloadJob(id, "processing", null, Instant.now()));
                try {
                    download(id, videoUrl);
                    jobs.put(id, new DownloadJob(id, "completed", null, Instant.now()));
                } catch (IOException | InterruptedException | RuntimeException ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    log.error("Gubb job failed jobId={}", id, ex);
                    jobs.put(id, new DownloadJob(id, "failed",
                            "Video download failed. Please try again.", Instant.now()));
                }
            });
        } catch (RejectedExecutionException ex) {
            jobs.remove(id);
            log.warn("Gubb download queue is full or shutting down jobId={}", id);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Download queue is full. Please try again later.", ex);
        }
        return job;
    }

    public DownloadJob getJob(String id) {
        DownloadJob job = jobs.get(id);
        if (job == null || isExpiredJob(job, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Download job not found or expired");
        }
        return job;
    }

    @PreDestroy
    public void shutdown() {
        downloads.shutdownNow();
    }

    private String download(String id, String videoUrl) throws IOException, InterruptedException {
        long started = System.nanoTime();
        log.info("Gubb download started jobId={}", id);
        Path workspace = Files.createTempDirectory(storageDirectory, ".download-");
        Process process = null;
        try {
            process = startDownload(workspace, videoUrl);
            waitForSuccess(process, workspace.resolve("yt-dlp.log"), "TikTok download");
            log.info("Gubb download finished; starting transcode jobId={}", id);
            process = startTranscode(workspace);
            waitForSuccess(process, workspace.resolve("ffmpeg.log"), "Video transcode");

            Path video = workspace.resolve("video.mp4");
            if (!Files.isRegularFile(video) || Files.size(video) == 0) {
                throw new IOException("FFmpeg did not produce an MP4 video");
            }
            Files.setLastModifiedTime(video, FileTime.from(Instant.now()));
            Files.move(video, storageDirectory.resolve(id + ".mp4"));
            log.info("Gubb video ready jobId={} durationMs={}",
                    id, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            return id;
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor();
            }
            deleteWorkspace(workspace);
        }
    }

    Process startDownload(Path workspace, String videoUrl) throws IOException {
        return new ProcessBuilder(
                "/usr/local/bin/yt-dlp",
                "--ignore-config",
                "--no-playlist",
                "--no-progress",
                "--format", "best[ext=mp4]/best",
                "--recode-video", "mp4",
                "--output", "download.%(ext)s",
                "--", videoUrl
        ).directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(workspace.resolve("yt-dlp.log").toFile())
                .start();
    }

    Process startTranscode(Path workspace) throws IOException {
        return new ProcessBuilder(
                "ffmpeg",
                "-nostdin",
                "-y",
                "-i", "download.mp4",
                "-map", "0:v:0",
                "-map", "0:a:0",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-movflags", "+faststart",
                "video.mp4"
        ).directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(workspace.resolve("ffmpeg.log").toFile())
                .start();
    }

    private static void waitForSuccess(Process process, Path logPath, String operation)
            throws IOException, InterruptedException {
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            throw new IOException(operation + " timed out");
        }
        if (process.exitValue() != 0) {
            String output;
            try (var log = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int length = log.read(buffer);
                output = length < 0 ? "" : new String(buffer, 0, length);
            }
            throw new IOException(operation + " failed: " + output);
        }
    }

    public Resource getVideo(String id) throws IOException {
        if (!isVideoId(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found");
        }
        Path video = storageDirectory.resolve(id + ".mp4");
        if (!Files.isRegularFile(video) || isExpired(video, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found or expired");
        }
        return new FileSystemResource(video);
    }

    @Scheduled(fixedDelay = 24 * 3_600_000)
    public void deleteExpiredVideos() throws IOException {
        Instant now = Instant.now();
        jobs.values().removeIf(job -> isExpiredJob(job, now));
        try (Stream<Path> files = Files.list(storageDirectory)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (Files.isRegularFile(file) && name.endsWith(".mp4")
                        && isVideoId(name.substring(0, name.length() - 4)) && isExpired(file, now)) {
                    Files.deleteIfExists(file);
                } else if (Files.isDirectory(file) && name.startsWith(".download-")
                        && isExpired(file, now)) {
                    deleteWorkspace(file);
                }
            }
        }
    }

    private boolean isExpired(Path file, Instant now) throws IOException {
        return !Files.getLastModifiedTime(file).toInstant().plus(RETENTION).isAfter(now);
    }

    private static boolean isExpiredJob(DownloadJob job, Instant now) {
        return ("completed".equals(job.status()) || "failed".equals(job.status()))
                && !job.updatedAt().plus(RETENTION).isAfter(now);
    }

    private static boolean isVideoId(String id) {
        return id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private static void deleteWorkspace(Path workspace) throws IOException {
        try (Stream<Path> files = Files.walk(workspace)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
