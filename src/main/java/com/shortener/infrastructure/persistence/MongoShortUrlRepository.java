package com.shortener.infrastructure.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.Optional;

public interface MongoShortUrlRepository extends MongoRepository<ShortUrlDocument, String> {
    Optional<ShortUrlDocument> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Query("{   'shortCode' : ?0  }")
    @Update("{  '$inc' : { 'clickCount' : 1 }  }") /*Prevents race conditions concurrency issue.*/
    void incrementClickCount(String shortCode);
}
