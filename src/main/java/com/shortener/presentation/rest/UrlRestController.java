package com.shortener.presentation.rest;

import com.shortener.application.dto.CreateShortUrlCommand;
import com.shortener.application.dto.ShortUrlResponse;
import com.shortener.application.service.UrlShortenerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@Tag(name = "Url Shortener", description = "Create and resolve short Urls")
public class UrlRestController {
    private final UrlShortenerApplicationService applicationService;

    public UrlRestController(UrlShortenerApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/api/v1/urls")
    @Operation(summary = "Create a short URL", description = "Generates a short code for the given original URL")
    @ApiResponse(responseCode = "201", description = "Short URL created")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlCommand command) {
        ShortUrlResponse response = applicationService.createShortUrl(command);
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    @GetMapping("/api/v1/urls/{code}")
    @Operation(summary = "Get short URL statistics")
    @ApiResponse(responseCode = "200", description = "Stats retrieved")
    @ApiResponse(responseCode = "404", description = "Short URL not found")
    public ResponseEntity<ShortUrlResponse> getStats(@PathVariable String code) {
        return ResponseEntity.ok(applicationService.getStats(code));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Redirect to original URL")
    @ApiResponse(responseCode = "302", description = "Redirect")
    @ApiResponse(responseCode = "404", description = "Short URL not found")
    public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException {
        String originalUrl = applicationService.resolveAndRedirect(code);
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", originalUrl);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }
}
