package com.taskmaster.shared.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API response envelope.
 *
 * @param <T> data payload type
 */
public record ApiResponse<T>(
    T data,
    Map<String, Object> meta
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Map.of("timestamp", Instant.now().toString()));
    }

    public static <T> ApiResponse<T> of(T data, Map<String, Object> additionalMeta) {
        return new ApiResponse<>(data, additionalMeta);
    }
}
