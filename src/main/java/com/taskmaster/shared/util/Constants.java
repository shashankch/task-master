package com.taskmaster.shared.util;

public final class Constants {

    private Constants() {
        // Utility class
    }

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
