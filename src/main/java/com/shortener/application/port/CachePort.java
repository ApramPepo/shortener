package com.shortener.application.port;

import com.shortener.domain.model.OriginalUrl;
import com.shortener.domain.model.ShortCode;
import java.util.Optional;

/**
 * Outbound port caching for Redis
 * Hexagonal Architecture to make the core app completely independent of Redis
 * In case we wanted to swap Redis with another Service
 * */


public interface CachePort {
    void put(ShortCode shortCode, OriginalUrl originalURL);
    Optional<OriginalUrl> get(ShortCode shortCode);
    void evict(ShortCode shortCode);
}
