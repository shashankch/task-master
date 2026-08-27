package com.taskmaster.shared.util;

/**
 * Global application constants for pagination, correlation, headers, and rate limiting.
 */
public final class Constants {

    private Constants() {
        // Utility class
    }

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    // Pagination Defaults
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Rate Limiting Key Prefixes
    public static final String RATE_LIMIT_AUTH_KEY_PREFIX = "rate_limit:auth:";
    public static final String RATE_LIMIT_GENERAL_KEY_PREFIX = "rate_limit:general:";

    // Storage Defaults
    public static final String ATTACHMENTS_PATH_PREFIX = "attachments/";
    public static final int DEFAULT_PRESIGNED_EXPIRATION_MINUTES = 15;
}
