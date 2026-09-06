package apiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ChatAnswerCache {

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final String keyPrefix;

    public ChatAnswerCache(
            StringRedisTemplate redisTemplate,
            @Value("${app.chat.cache.ttl:PT24H}") Duration ttl,
            @Value("${app.chat.cache.key-prefix:chat:answer:}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
        this.keyPrefix = keyPrefix;
    }

    public String get(String question) {
        return redisTemplate.opsForValue().get(keyFor(question));
    }

    public void put(String question, String answer) {
        redisTemplate.opsForValue().set(keyFor(question), answer, ttl);
    }

    private String keyFor(String question) {
        return keyPrefix + sha256(normalize(question));
    }

    private String normalize(String question) {
        return question.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
