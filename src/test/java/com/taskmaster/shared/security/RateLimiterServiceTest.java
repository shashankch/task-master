package com.taskmaster.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmaster.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(null, true, 3);
    }

    @Test
    @DisplayName("Should allow requests within limit")
    void checkRateLimit_WhenUnderLimit_ShouldAllow() {
        String key = "test-key-1";
        assertThatCode(() -> {
            rateLimiterService.checkRateLimit(key, 3, 60);
            rateLimiterService.checkRateLimit(key, 3, 60);
            rateLimiterService.checkRateLimit(key, 3, 60);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when exceeding limit")
    void checkRateLimit_WhenExceedingLimit_ShouldThrow() {
        String key = "test-key-2";
        rateLimiterService.checkRateLimit(key, 2, 60);
        rateLimiterService.checkRateLimit(key, 2, 60);

        assertThatThrownBy(() -> rateLimiterService.checkRateLimit(key, 2, 60))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("Too many requests");
    }
}
