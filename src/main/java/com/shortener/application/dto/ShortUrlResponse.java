package com.shortener.application.dto;

import java.time.Instant;

public record ShortUrlResponse (
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        long clickCount
) {}
