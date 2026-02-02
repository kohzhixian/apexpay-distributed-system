package com.apexpay.apigateway.constants;

/**
 * Fallback error messages for circuit breaker responses.
 */
public final class FallbackMessages {
    private FallbackMessages() {} // Prevent instantiation

    public static final String USER_SERVICE_UNAVAILABLE = "The User Service is currently taking too long to respond or is down.";
    public static final String WALLET_SERVICE_UNAVAILABLE = "The Wallet Service is currently taking too long to respond or is down.";
    public static final String PAYMENT_SERVICE_UNAVAILABLE = "The Payment Service is currently taking too long to respond or is down.";
}
