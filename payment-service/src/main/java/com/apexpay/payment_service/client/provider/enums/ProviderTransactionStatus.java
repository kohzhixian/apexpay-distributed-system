package com.apexpay.payment_service.client.provider.enums;

/**
 * Enumeration of payment provider transaction statuses.
 * <p>
 * Represents the possible states of a transaction as reported by the
 * payment provider.
 * </p>
 */
public enum ProviderTransactionStatus {
    /** Transaction successfully completed */
    SUCCESS,
    /** Transaction submitted but not yet confirmed */
    PENDING,
    /** Transaction failed */
    FAILED
}
