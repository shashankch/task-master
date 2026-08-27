package com.taskmaster.shared.constant;

/**
 * Constants related to security, JWT tokens, headers, and authentication filter configurations.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String NATIVE_TOKEN_HEADER = "token";

    // JWT Claims
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_USER_ID = "userId";
    public static final String ROLE_PREFIX = "ROLE_";

    // Public Endpoint Matchers
    public static final String[] PUBLIC_AUTH_ENDPOINTS = {
        "/api/v1/auth/**"
    };

    public static final String[] PUBLIC_SWAGGER_ENDPOINTS = {
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    };

    public static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {
        "/actuator/**"
    };

    public static final String[] PUBLIC_WS_ENDPOINTS = {
        "/ws/**"
    };
}
