package com.shortener.domain.model;

import java.time.Instant;
import java.util.Objects;

public class ShortUrl {
    private final ShortCode shortCode;
    private final OriginalUrl originalUrl;
    private final Instant createdAt;
    private long clickCount;

    /*New instance*/
    public ShortUrl(ShortCode shortCode, OriginalUrl originalUrl) {
        this.shortCode = Objects.requireNonNull(shortCode, "shortCode must not be null");
        this.originalUrl = Objects.requireNonNull(originalUrl, "originalUrl must not be null");
        this.createdAt = Instant.now();
        this.clickCount = 0;
    }

    /*Existing data Retrieval*/
    public ShortUrl(ShortCode shortCode, OriginalUrl originalUrl, Instant createdAt, long clickCount) {
        this.shortCode = Objects.requireNonNull(shortCode);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.clickCount = clickCount;
    }

    public void registerClick() {
        this.clickCount++;
    }

    public ShortCode getShortCode() {
        return shortCode;
    }

    public OriginalUrl getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortUrl that)) return false;
        return shortCode.equals(that.shortCode);
    }

    @Override
    public int hashCode() {         return shortCode.hashCode();        }
}
