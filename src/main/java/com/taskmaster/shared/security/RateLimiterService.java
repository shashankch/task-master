package com.taskmaster.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.taskmaster.shared.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Distributed sliding-window rate limiter using Redis with Caffeine-backed bounded in-memory fallback.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int defaultMaxRequests;
    private final int defaultWindowSeconds;

    // Bounded in-memory LRU cache with TTL eviction to prevent memory leakage in fallback mode
    private final Cache<String, ConcurrentLinkedQueue<Long>> inMemoryBuckets = Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();

    public RateLimiterService(
        @Autowired(required = false) StringRedisTemplate redisTemplate,
        @Value("${app.rate-limiting.enabled:true}") boolean enabled,
        @Value("${app.rate-limiting.auth-requests-per-minute:5}") int defaultMaxRequests
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultWindowSeconds = 60;
    }

    public void checkRateLimit(String key) {
        checkRateLimit(key, defaultMaxRequests, defaultWindowSeconds);
    }

    public void checkRateLimit(String key, int maxRequests, int windowSeconds) {
        if (!enabled) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:" + key;
                redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
                Long currentCount = redisTemplate.opsForZSet().zCard(redisKey);

                if (currentCount != null && currentCount >= maxRequests) {
                    throw new RateLimitExceededException("Too many requests. Please try again later.", windowSeconds);
                }

                String member = now + ":" + UUID.randomUUID();
                redisTemplate.opsForZSet().add(redisKey, member, now);
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds * 2L));
                return;
            } catch (RateLimitExceededException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis rate limiter failed, falling back to in-memory: {}", e.getMessage());
            }
        }

        // Bounded in-memory sliding window implementation with automatic eviction
        ConcurrentLinkedQueue<Long> timestamps = inMemoryBuckets.get(key, k -> new ConcurrentLinkedQueue<>());
        if (timestamps != null) {
            Long timestamp;
            while ((timestamp = timestamps.peek()) != null && timestamp < windowStart) {
                timestamps.poll();
            }

            if (timestamps.size() >= maxRequests) {
                throw new RateLimitExceededException("Too many requests. Please try again later.", windowSeconds);
            }

            timestamps.add(now);
        }
    }
}
