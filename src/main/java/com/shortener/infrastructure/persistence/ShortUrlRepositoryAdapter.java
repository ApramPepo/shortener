package com.shortener.infrastructure.persistence;

import com.shortener.domain.model.ShortCode;
import com.shortener.domain.model.ShortUrl;
import com.shortener.domain.model.OriginalUrl;
import com.shortener.domain.repository.ShortUrlRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class ShortUrlRepositoryAdapter implements ShortUrlRepository {

    private final MongoShortUrlRepository mongoRepository;

    public ShortUrlRepositoryAdapter(MongoShortUrlRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public ShortUrl save(ShortUrl shortURL) {
        ShortUrlDocument doc = new ShortUrlDocument(
                shortURL.getShortCode().value(),
                shortURL.getOriginalUrl().value(),
                shortURL.getCreatedAt(),
                shortURL.getClickCount()
        );
        ShortUrlDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<ShortUrl> findByShortCode(ShortCode shortCode) {
        return mongoRepository.findByShortCode(shortCode.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByShortCode(ShortCode shortCode) {
        return mongoRepository.existsByShortCode(shortCode.value());
    }

    @Override
    public void incrementClickCount(ShortCode shortCode) {
        mongoRepository.incrementClickCount(shortCode.value());
    }

    private ShortUrl toDomain(ShortUrlDocument doc) {
        return new ShortUrl(
                ShortCode.of(doc.getShortCode()),
                OriginalUrl.of(doc.getOriginalUrl()),
                doc.getCreatedAt(),
                doc.getClickCount()
        );
    }

}
