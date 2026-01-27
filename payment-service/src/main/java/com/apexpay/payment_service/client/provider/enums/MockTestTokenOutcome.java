package com.apexpay.payment_service.client.provider.enums;

/**
 * Supported deterministic outcomes for mock test tokens.
 */
public enum MockTestTokenOutcome {
    SUCCESS,
    CARD_DECLINED,
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD,
    FRAUD_SUSPECTED,
    NETWORK_ERROR,
    PROVIDER_UNAVAILABLE,
    RATE_LIMITED,
    SLOW_RESPONSE,
    PENDING
}
