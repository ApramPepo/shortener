package com.shortener.domain.model;

import java.util.regex.Pattern;

public record ShortCode(String value) {
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,12}$");

    public ShortCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Short code must not be blank");
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid short code format: " + value);
        }
    }

    public static ShortCode of(String value) {      return new ShortCode(value);      }
}
