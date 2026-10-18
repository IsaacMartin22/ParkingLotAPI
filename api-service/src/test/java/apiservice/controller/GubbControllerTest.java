package apiservice.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GubbControllerTest {
    private static final String TIKTOK_URL =
            "https://www.tiktok.com/@hunainn/video/7696938907090439444";

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
