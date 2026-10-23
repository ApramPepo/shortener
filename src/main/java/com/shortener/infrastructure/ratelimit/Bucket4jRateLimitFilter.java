package com.shortener.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component /*Spring Bean*/
@Order(1) /*High Priority*/
public class Bucket4jRateLimitFilter extends OncePerRequestFilter /*Executes once per HTTP request*/ {

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> configSupplier;
    private final long capacity;

    public Bucket4jRateLimitFilter(
            /*Config Injection*/
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${rate-limit.capacity:100}") long capacity,
            @Value("${rate-limit.refill-tokens:100}") long refillTokens,
            @Value("${rate-limit.refill-duration:1m}") Duration refillDuration) {

        this.capacity = capacity;

        /*Config to init Bucket4j backend by Redis*/
        RedisClient redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection).withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(refillDuration)
        ).build();

        this.configSupplier = () -> BucketConfiguration.builder().addLimit(
                Bandwidth.builder().capacity(capacity).refillGreedy(refillTokens, refillDuration).build()
        ).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        /*Checks the URL path of the incoming request*/
        /*Skips actuator, swagger, or api-docs entirely*/
        String path = request.getRequestURI();
        if(path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
        }

        String clientIp = extractClientIp(request);
        Bucket bucket = proxyManager.builder().build(clientIp, configSupplier);

        if(bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(capacity));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(capacity));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"status\":429}");
        }

    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardFor = request.getHeader("X-Forward-For");
        if(xForwardFor != null && !xForwardFor.isBlank()) {
            return xForwardFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
