package com.shortener.application.service;

import com.shortener.application.dto.CreateShortUrlCommand;
import com.shortener.application.dto.ShortUrlResponse;
import com.shortener.application.port.CachePort;
import com.shortener.domain.exception.UrlNotFoundException;
import com.shortener.domain.model.OriginalUrl;
import com.shortener.domain.model.ShortCode;
import com.shortener.domain.model.ShortUrl;
import com.shortener.domain.repository.ShortUrlRepository;
import com.shortener.domain.service.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlShortenerApplicationService {
    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CachePort cachePort;
    private final String baseUrl;

    public UrlShortenerApplicationService(
            ShortUrlRepository shortUrlRepository, ShortCodeGenerator shortCodeGenerator,
            CachePort cachePort, @Value("${app.base-url}") String baseUrl) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.cachePort = cachePort;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public ShortUrlResponse createShortUrl(CreateShortUrlCommand command) {
        OriginalUrl originalURL = OriginalUrl.of(command.originalUrl());
        ShortCode shortCode;

        int attempts = 0;
        do {
            shortCode = shortCodeGenerator.generate();
            attempts++;
            if(attempts > 10) {
                throw new IllegalStateException("Failed to generate unique short code after 10 attempts");
            }
        } while (shortUrlRepository.existsByShortCode(shortCode));

        ShortUrl shortURL = new ShortUrl(shortCode, originalURL);
        ShortUrl saved = shortUrlRepository.save(shortURL);

        cachePort.put(saved.getShortCode(), saved.getOriginalUrl());

        return toResponse(saved);
    }

    public String resolveAndRedirect(String code) {
        ShortCode shortCode = ShortCode.of(code);
        var cached = cachePort.get(shortCode); /*Caching aside*/

        if (cached.isPresent()) {
            shortUrlRepository.incrementClickCount(shortCode);
            return cached.get().value();
        }

        ShortUrl shortURL = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(code));

        cachePort.put(shortURL.getShortCode(), shortURL.getOriginalUrl());
        shortUrlRepository.incrementClickCount(shortCode);

        return shortURL.getOriginalUrl().value();
    }

    public ShortUrlResponse getStats(String code) {
        ShortCode shortCode = ShortCode.of(code);
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(code));
        return toResponse(shortUrl);
    }

    private ShortUrlResponse toResponse(ShortUrl shortURL) {
        return new ShortUrlResponse(
                shortURL.getShortCode().value(),
                baseUrl + "/" +shortURL.getShortCode().value(),
                shortURL.getOriginalUrl().value(),
                shortURL.getCreatedAt(),
                shortURL.getClickCount()
        );
    }
}
