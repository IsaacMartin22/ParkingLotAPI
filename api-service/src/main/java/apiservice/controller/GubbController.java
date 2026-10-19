package apiservice.controller;


import apiservice.service.GubbVideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import parkinglot.common.response.GubbResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

// curl -v -G "https://api.isaaccmart.in/api/gubb" --data-urlencode "url=https://www.tiktok.com/@hunainn/video/7696938907090439444"

@RestController
@RequestMapping("/api/gubb")
public class GubbController {
    private final GubbVideoService videos;

    public GubbController(GubbVideoService videos) {
        this.videos = videos;
    }

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

        try {
            validateTikTokUrl(videoUrl);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TikTok URL", ex);
        }

        String id = videos.download(videoUrl);
        return new GubbResponse(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/videos/{id}")
                .buildAndExpand(id)
                .toUriString());
    }

    @GetMapping("/videos/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String id) throws IOException {
        // Spring MVC handles HTTP Range requests for file resources, including browser seeking.
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/mp4"))
                .cacheControl(CacheControl.noStore())
                .body(videos.getVideo(id));
    }

    static void validateTikTokUrl(String videoUrl) {
        URI uri = URI.create(videoUrl);

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !ALLOWED_HOSTS.contains(
                uri.getHost().toLowerCase(Locale.ROOT))
                || uri.getRawUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException(
                    "Invalid TikTok URL");
        }
    }
}
