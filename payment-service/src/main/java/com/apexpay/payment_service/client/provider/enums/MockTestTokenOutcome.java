package com.apexpay.payment_service.client.provider.enums;

/**
 * Deterministic outcomes for mock test tokens.
 * <p>
 * Each outcome maps to a specific test token (e.g., tok_visa_success)
 * allowing predictable testing of payment flows.
 * </p>
 */
public enum MockTestTokenOutcome {
    /** Payment succeeds immediately */
    SUCCESS,
    /** Card is declined by issuing bank */
    CARD_DECLINED,
    /** Card has insufficient funds */
    INSUFFICIENT_FUNDS,
    /** Card has expired */
    EXPIRED_CARD,
    /** Card number is invalid */
    INVALID_CARD,
    /** Transaction flagged for potential fraud */
    FRAUD_SUSPECTED,
    /** Network timeout connecting to processor */
    NETWORK_ERROR,
    /** Payment provider is unavailable */
    PROVIDER_UNAVAILABLE,
    /** Rate limit exceeded */
    RATE_LIMITED,
    /** Simulates slow response for timeout testing */
    SLOW_RESPONSE,
    /** Payment is pending external confirmation */
    PENDING
}
