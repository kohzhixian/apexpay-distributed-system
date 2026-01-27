package com.apexpay.payment_service.client.provider.enums;

/**
 * Enumeration of payment provider failure codes.
 * <p>
 * Represents various failure scenarios that can occur when processing
 * payments through external providers. These codes help categorize failures
 * for logging, analytics, and retry logic.
 * </p>
 */
public enum ProviderFailureCode {
    /** Payment card was declined by the bank or card issuer */
    CARD_DECLINED("card_declined"),
    /** Insufficient funds in the payment method */
    INSUFFICIENT_FUNDS("insufficient_funds"),
    /** Payment card has expired */
    EXPIRED_CARD("expired_card"),
    /** Payment card number or details are invalid */
    INVALID_CARD("invalid_card"),
    /** Transaction flagged as potentially fraudulent */
    FRAUD_SUSPECTED("fraud_suspected"),
    /** Network communication error (retryable) */
    NETWORK_ERROR("network_error"),
    /** Payment provider service is unavailable (retryable) */
    PROVIDER_UNAVAILABLE("provider_unavailable"),
    /** Rate limit exceeded, too many requests (retryable) */
    RATE_LIMITED("rate_limited"),
    /** Transaction not found when querying status */
    TRANSACTION_NOT_FOUND("transaction_not_found");

    private final String code;

    ProviderFailureCode(String code) {
        this.code = code;
    }

    /**
     * Returns the string code for this failure code.
     *
     * @return the failure code as a string
     */
    public String getCode() {
        return code;
    }
}
