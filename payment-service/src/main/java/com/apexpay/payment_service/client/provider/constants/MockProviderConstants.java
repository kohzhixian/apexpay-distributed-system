package com.apexpay.payment_service.client.provider.constants;

/**
 * Constants for the mock payment provider used in testing and development.
 */
public final class MockProviderConstants {
    private MockProviderConstants() {} // Prevent instantiation

    // Provider identification
    public static final String PROVIDER_NAME = "MOCK";
    public static final String TRANSACTION_ID_PREFIX = "mock_ch_";

    // Test tokens for deterministic testing
    public static final String TOKEN_VISA_SUCCESS = "tok_visa_success";
    public static final String TOKEN_CARD_DECLINED = "tok_card_declined";
    public static final String TOKEN_INSUFFICIENT_FUNDS = "tok_insufficient_funds";
    public static final String TOKEN_EXPIRED_CARD = "tok_expired_card";
    public static final String TOKEN_INVALID_CARD = "tok_invalid_card";
    public static final String TOKEN_FRAUD_SUSPECTED = "tok_fraud_suspected";
    public static final String TOKEN_NETWORK_ERROR = "tok_network_error";
    public static final String TOKEN_PROVIDER_UNAVAILABLE = "tok_provider_unavailable";
    public static final String TOKEN_RATE_LIMITED = "tok_rate_limited";
    public static final String TOKEN_SLOW_RESPONSE = "tok_slow_response";
    public static final String TOKEN_PENDING = "tok_pending";

    // Failure messages
    public static final String MSG_CARD_DECLINED = "Your card was declined";
    public static final String MSG_CARD_DECLINED_BY_BANK = "Card was declined by issuing bank";
    public static final String MSG_INSUFFICIENT_FUNDS = "Insufficient funds on card";
    public static final String MSG_INSUFFICIENT_FUNDS_SHORT = "Insufficient funds";
    public static final String MSG_CARD_EXPIRED = "Card has expired";
    public static final String MSG_INVALID_CARD = "Card number is invalid";
    public static final String MSG_FRAUD_SUSPECTED = "Transaction flagged for potential fraud";
    public static final String MSG_NETWORK_TIMEOUT = "Network timeout connecting to payment processor";
    public static final String MSG_NETWORK_TIMEOUT_SHORT = "Network timeout";
    public static final String MSG_PROVIDER_UNAVAILABLE = "Payment provider is temporarily unavailable";
    public static final String MSG_SERVICE_UNAVAILABLE = "Service temporarily unavailable";
    public static final String MSG_RATE_LIMITED = "Rate limit exceeded";
    public static final String MSG_TRANSACTION_NOT_FOUND = "Transaction not found: %s";
}
