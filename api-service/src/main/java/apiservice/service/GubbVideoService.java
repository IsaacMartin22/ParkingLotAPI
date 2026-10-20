package apiservice.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

@Service
public class GubbVideoService {
    private static final Logger log = LoggerFactory.getLogger(GubbVideoService.class);
    private static final Duration RETENTION = Duration.ofDays(2);
    private static final long POLL_TIMEOUT_NANOS = Duration.ofSeconds(30).toNanos();
    private final Path storageDirectory;
    private final LongSupplier nanoTime;
    private final ConcurrentMap<String, JobState> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor downloads = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), task -> {
                Thread worker = new Thread(task, "gubb-download");
                worker.setPriority(Thread.MIN_PRIORITY);
                return worker;
            });

    public record DownloadJob(String id, String status, String error, Instant updatedAt) {
    }

    // jobStatus/jobUpdatedAt are null when the in-memory job is gone (e.g. after a restart).
    public record StoredVideo(String id, String path, long sizeBytes, Instant storedAt,
                              Instant expiresAt, boolean expired, String jobStatus,
                              Instant jobUpdatedAt) {
    }

    private final class JobState {
        private DownloadJob snapshot;
        private long lastPolled;
        private final FutureTask<Void> task;

        private JobState(String id, String videoUrl) {
            snapshot = new DownloadJob(id, "queued", null, Instant.now());
            lastPolled = nanoTime.getAsLong();
            task = new FutureTask<>(() -> {
                runJob(this, videoUrl);
                return null;
            });
        }
    }

    @Autowired
    public GubbVideoService(@Value("${app.gubb.storage-directory}") String storageDirectory)
            throws IOException {
        this(storageDirectory, System::nanoTime);
    }

    GubbVideoService(String storageDirectory, LongSupplier nanoTime) throws IOException {
        this.nanoTime = nanoTime;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.storageDirectory);
    }

    public String download(String videoUrl) throws IOException, InterruptedException {
        return download(UUID.randomUUID().toString(), videoUrl, null);
    }

    public DownloadJob startJob(String videoUrl) {
        String id = UUID.randomUUID().toString();
        JobState job = new JobState(id, videoUrl);
        jobs.put(id, job);
        try {
            downloads.execute(job.task);
        } catch (RejectedExecutionException ex) {
            jobs.remove(id);
            log.warn("Gubb download queue is full or shutting down jobId={}", id);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Download queue is full. Please try again later.", ex);
        }
        synchronized (job) {
            return job.snapshot;
        }
    }

    private void runJob(JobState job, String videoUrl) {
        String id;
        try {
            synchronized (job) {
                checkActive(job);
                id = job.snapshot.id();
                job.snapshot = new DownloadJob(id, "processing", null, Instant.now());
            }
            download(id, videoUrl, job);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            synchronized (job) {
                if (isPending(job.snapshot)) {
                    log.error("Gubb job failed jobId={}", job.snapshot.id(), ex);
                    job.snapshot = new DownloadJob(job.snapshot.id(), "failed",
                            "Video download failed. Please try again.", Instant.now());
                } else if (!(ex instanceof InterruptedException || ex instanceof CancellationException)) {
                    log.warn("Gubb job cleanup failed jobId={}", job.snapshot.id(), ex);
                }
            }
        }
    }

    public DownloadJob getJob(String id) {
        JobState job = findJob(id);
        synchronized (job) {
            cancelIfUnpolled(job);
            job.lastPolled = nanoTime.getAsLong();
            return job.snapshot;
        }
    }

    public DownloadJob cancelJob(String id) {
        JobState job = findJob(id);
        synchronized (job) {
            cancel(job, "Video download cancelled.");
            return job.snapshot;
        }
    }

    private JobState findJob(String id) {
        JobState job = jobs.get(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Download job not found or expired");
        }
        synchronized (job) {
            if (isExpiredJob(job.snapshot, Instant.now())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Download job not found or expired");
            }
        }
        return job;
    }

    @Scheduled(fixedDelay = 1000)
    public void cancelUnpolledJobs() {
        jobs.values().forEach(job -> {
            synchronized (job) {
                cancelIfUnpolled(job);
            }
        });
    }

    private void cancelIfUnpolled(JobState job) {
        if (nanoTime.getAsLong() - job.lastPolled >= POLL_TIMEOUT_NANOS) {
            cancel(job, "Video download cancelled because polling stopped.");
        }
    }

    // Call only while holding the job monitor; terminal states must never be overwritten.
    private void cancel(JobState job, String reason) {
        if (isPending(job.snapshot)) {
            job.snapshot = new DownloadJob(job.snapshot.id(), "cancelled", reason, Instant.now());
            job.task.cancel(true);
            downloads.remove(job.task);
            log.info("Gubb job cancelled jobId={} reason={}", job.snapshot.id(), reason);
        }
    }

    private void checkActive(JobState job) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Video download interrupted");
        }
        if (job != null) {
            synchronized (job) {
                cancelIfUnpolled(job);
                if ("cancelled".equals(job.snapshot.status())) {
                    throw new CancellationException(job.snapshot.error());
                }
            }
        }
    }

    private static boolean isPending(DownloadJob job) {
        return "queued".equals(job.status()) || "processing".equals(job.status());
    }

    @PreDestroy
    public void shutdown() {
        downloads.shutdown();
        jobs.values().forEach(job -> {
            synchronized (job) {
                cancel(job, "Video download cancelled because the server is shutting down.");
            }
        });
        downloads.shutdownNow();
    }

    private String download(String id, String videoUrl, JobState job) throws IOException, InterruptedException {
        checkActive(job);
        long started = System.nanoTime();
        log.info("Gubb download started jobId={}", id);
        Path workspace = Files.createTempDirectory(storageDirectory, ".download-");
        // Job IDs are UUIDs, so each published file gets a unique name in the shared storage directory.
        String fileName = id + ".mp4";
        Process process = null;
        try {
            synchronized (job == null ? workspace : job) {
                checkActive(job);
                process = startDownload(workspace, fileName, videoUrl);
            }
            waitForSuccess(process, workspace.resolve("yt-dlp.log"), "TikTok download");
            checkActive(job);

            Path video = workspace.resolve(fileName);
            if (!Files.isRegularFile(video) || Files.size(video) == 0) {
                throw new IOException("yt-dlp did not produce an MP4 video");
            }
            // Serialize publication with cancellation so cancelled jobs cannot publish a video.
            synchronized (job == null ? workspace : job) {
                checkActive(job);
                Files.setLastModifiedTime(video, FileTime.from(Instant.now()));
                Files.move(video, storageDirectory.resolve(fileName));
                if (job != null) {
                    job.snapshot = new DownloadJob(id, "completed", null, Instant.now());
                }
            }
            log.info("Gubb video ready jobId={} durationMs={}",
                    id, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            return id;
        } finally {
            try {
                if (process != null) {
                    stopProcess(process);
                }
            } finally {
                deleteWorkspace(workspace);
            }
        }
    }

    private static void stopProcess(Process process) throws IOException, InterruptedException {
        try (Stream<ProcessHandle> descendants = process.descendants()) {
            descendants.forEach(ProcessHandle::destroyForcibly);
        }
        if (process.isAlive()) {
            process.destroyForcibly();
            boolean interrupted = Thread.interrupted();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    throw new IOException("Video process did not stop after cancellation");
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // yt-dlp --ignore-config --no-playlist --no-progress --fixup never --format best[vcodec^=avc1][ext=mp4]/best[vcodec^=h264][ext=mp4] --limit-rate 2M --socket-timeout 15 --retries 2 --output amogus.mp4 https://www.tiktok.com/@hunainn/video/7696938907090439444

    // yt-dlp --ignore-config --no-playlist -F "https://www.tiktok.com/@hunainn/video/7696938907090439444"
    Process startDownload(Path workspace, String fileName, String videoUrl) throws IOException {
        return new ProcessBuilder(
                "nice", "-n", "19", "ionice", "-c", "3",
                "/usr/local/bin/yt-dlp",
                "--ignore-config",
                "--no-playlist",
                "--no-progress",
                "--fixup", "never",
                "--format", "best[vcodec^=avc1][ext=mp4]/best[vcodec^=h264][ext=mp4]",
                "--limit-rate", "2M",
                "--socket-timeout", "15",
                "--retries", "2",
                "--output", fileName,
                "--", videoUrl
        ).directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(workspace.resolve("yt-dlp.log").toFile())
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

    public List<StoredVideo> listVideos() throws IOException {
        Instant now = Instant.now();
        List<StoredVideo> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(storageDirectory)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                String id = name.endsWith(".mp4") ? name.substring(0, name.length() - 4) : "";
                if (!isVideoId(id)) {
                    continue;
                }
                BasicFileAttributes attributes;
                try {
                    attributes = Files.readAttributes(file, BasicFileAttributes.class);
                } catch (NoSuchFileException ex) {
                    continue;
                }
                if (!attributes.isRegularFile()) {
                    continue;
                }
                Instant storedAt = attributes.lastModifiedTime().toInstant();
                Instant expiresAt = storedAt.plus(RETENTION);
                DownloadJob job = peekJob(id);
                result.add(new StoredVideo(id, file.toString(), attributes.size(), storedAt,
                        expiresAt, !expiresAt.isAfter(now),
                        job == null ? null : job.status(), job == null ? null : job.updatedAt()));
            }
        }
        result.sort(Comparator.comparing(StoredVideo::expiresAt));
        return result;
    }

    public void deleteVideo(String id) throws IOException {
        if (!isVideoId(id) || !Files.deleteIfExists(storageDirectory.resolve(id + ".mp4"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found");
        }
        // Drop the job too so its status endpoint stops advertising a video URL that no longer exists.
        jobs.remove(id);
        log.info("Gubb video deleted manually jobId={}", id);
    }

    // Read-only lookup: unlike getJob, this does not count as a client poll.
    private DownloadJob peekJob(String id) {
        JobState job = jobs.get(id);
        if (job == null) {
            return null;
        }
        synchronized (job) {
            return job.snapshot;
        }
    }

    @Scheduled(fixedDelay = 24 * 3_600_000)
    public void deleteExpiredVideos() throws IOException {
        Instant now = Instant.now();
        jobs.values().removeIf(job -> {
            synchronized (job) {
                return isExpiredJob(job.snapshot, now);
            }
        });
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
        return !isPending(job)
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
