package com.apexpay.wallet_service.enums;

/**
 * Enumeration of wallet transaction status values.
 * <p>
 * Represents the lifecycle states of a wallet transaction:
 * <ul>
 * <li>PENDING - Transaction created but not yet finalized (e.g., reserved funds)</li>
 * <li>COMPLETED - Transaction successfully completed</li>
 * <li>CANCELLED - Transaction was cancelled and funds released</li>
 * </ul>
 * </p>
 */
public enum WalletTransactionStatusEnum {
    /** Transaction created but not yet finalized (e.g., reserved funds) */
    PENDING,
    /** Transaction successfully completed */
    COMPLETED,
    /** Transaction was cancelled and funds released */
    CANCELLED
}
