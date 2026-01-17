package com.apexpay.common.constants;

/**
 * Custom HTTP header constants for inter-service communication.
 * Used by API Gateway to propagate user identity to downstream services.
 */
public final class HttpHeaders {

    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USER_EMAIL = "X-User-Email";
    public static final String X_USER_NAME = "X-User-Name";

    private HttpHeaders() {
        // Prevent instantiation
    }
}
