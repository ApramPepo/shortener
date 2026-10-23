package com.shortener.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlCommand (
    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL too long")
    String originalUrl
) {}
