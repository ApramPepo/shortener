package com.shortener.domain.repository;

import com.shortener.domain.model.ShortCode;
import com.shortener.domain.model.ShortUrl;
import java.util.Optional;

/**
 * Domain repository
 * Data access layer */

public interface ShortUrlRepository {

    ShortUrl save(ShortUrl shortURL);

    Optional<ShortUrl> findByShortCode(ShortCode shortCode);

    boolean existsByShortCode(ShortCode shortCode);

    void incrementClickCount(ShortCode shortCode);
}
