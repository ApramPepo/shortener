package com.shortener.infrastructure.cache;

import com.shortener.application.port.CachePort;
import com.shortener.domain.model.ShortCode;
import com.shortener.domain.model.OriginalUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.time.Duration;

@Component
public class RedisCacheAdapter implements CachePort {
    private static final String KEY_PREFIX = "shorturl:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisCacheAdapter(StringRedisTemplate redisTemplate, @Value("${app.cache.ttl:24h}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public void put(ShortCode shortCode, OriginalUrl originalURL) {
        redisTemplate.opsForValue().set(buildKey(shortCode), originalURL.value(), ttl);
    }

    @Override
    public Optional<OriginalUrl> get(ShortCode shortCode) {
        String value = redisTemplate.opsForValue().get(buildKey(shortCode));
        return value != null ? Optional.of(OriginalUrl.of(value)) : Optional.empty();
    }

    @Override
    public void evict(ShortCode shortCode) {
        redisTemplate.delete(buildKey(shortCode));
    }

    private String buildKey(ShortCode shortCode) {
        return KEY_PREFIX + shortCode.value();
    }

}
