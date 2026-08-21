package com.taskmaster.user.application.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    String tokenType,
    UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, expiresInSeconds, "Bearer", user);
    }
}
