package apiservice.controller;


import org.springframework.web.bind.annotation.*;
import parkinglot.common.response.GubbResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/gubb")
public class GubbController {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "tiktok.com",
            "www.tiktok.com",
            "m.tiktok.com",
            "vm.tiktok.com",
            "vt.tiktok.com"
    );

    @GetMapping
    public GubbResponse getDownloadUrl(@RequestParam("url") String videoUrl)
            throws IOException, InterruptedException {

        validateTikTokUrl(videoUrl);

        Process process = new ProcessBuilder(
                "yt-dlp",
                "--no-playlist",
                "--format", "best[ext=mp4]/best",
                "--get-url",
                videoUrl
        )
                .redirectErrorStream(true)
                .start();

        try {
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException(
                        "TikTok extraction timed out");
            }

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            if (process.exitValue() != 0 || output.isEmpty()) {
                throw new IOException(
                        "Failed to extract TikTok video: " + output);
            }

            // The first line is the resolved media URL.
            return new GubbResponse(output.lines()
                    .filter(line -> line.startsWith("https://"))
                    .findFirst()
                    .orElseThrow(() ->
                            new IOException("No media URL returned")));

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static void validateTikTokUrl(String videoUrl) {
        URI uri = URI.create(videoUrl);

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !ALLOWED_HOSTS.contains(
                uri.getHost().toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid TikTok URL");
        }
    }
}
