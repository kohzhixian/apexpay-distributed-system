package com.apexpay.userservice.constants;

/**
 * Constants for authentication and authorization in user-service.
 */
public final class AuthConstants {
    private AuthConstants() {} // Prevent instantiation

    // Cookie names
    public static final String COOKIE_ACCESS_TOKEN = "access_token";
    public static final String COOKIE_REFRESH_TOKEN = "refresh_token";

    // Cookie configuration
    public static final String COOKIE_PATH_ROOT = "/";
    public static final String COOKIE_PATH_REFRESH = "/api/v1/auth/refresh";
    public static final String COOKIE_SAME_SITE_STRICT = "Strict";

    // Token configuration
    public static final String REFRESH_TOKEN_SEPARATOR = ":";

    // JWT configuration
    public static final String JWT_CLAIM_EMAIL = "email";
    public static final String JWT_CLAIM_USERNAME = "username";
    public static final String JWT_ISSUER = "apexpay-user-service";
    public static final String JWT_AUDIENCE = "apexpay-api";

    // Key formats
    public static final String PKCS1_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String PKCS8_PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    public static final String PKCS8_PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";
    public static final String KEY_ALGORITHM_RSA = "RSA";

    // Roles
    public static final String ROLE_USER = "USER";
}
