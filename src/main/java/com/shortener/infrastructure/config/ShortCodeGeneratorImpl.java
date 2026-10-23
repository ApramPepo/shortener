package com.shortener.infrastructure.config;

import com.shortener.domain.model.ShortCode;
import com.shortener.domain.service.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGeneratorImpl implements ShortCodeGenerator {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGeneratorImpl(@Value("${app.short-code-length}") int length) {
        this.length = length;
    }

    @Override
    public ShortCode generate() {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return ShortCode.of(sb.toString());
    }
}
