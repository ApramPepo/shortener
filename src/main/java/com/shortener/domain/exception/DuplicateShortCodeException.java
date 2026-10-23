package com.shortener.domain.exception;

public class DuplicateShortCodeException extends RuntimeException {
    public DuplicateShortCodeException(String shortCode) {
        super("Short code already exists: " + shortCode);
    }
}
