package com.shortener.domain.model;

import java.net.URI;
import java.net.URISyntaxException;

public record OriginalUrl(String value) {

    public OriginalUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                throw new IllegalArgumentException("Only http/https schemes are allowed");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + value, e);
        }
    }

    public static OriginalUrl of(String value) {    return new OriginalUrl(value);      }
}
