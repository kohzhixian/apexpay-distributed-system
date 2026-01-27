package com.apexpay.wallet_service.enums;

/**
 * Enumeration of wallet transaction types.
 * <p>
 * Represents the direction and nature of a wallet transaction:
 * <ul>
 * <li>CREDIT - Funds added to wallet (top-up, transfer received)</li>
 * <li>DEBIT - Funds deducted from wallet (transfer sent, payment)</li>
 * <li>RESERVE - Funds reserved for pending payment (legacy, use DEBIT with PENDING status)</li>
 * </ul>
 * </p>
 */
public enum TransactionTypeEnum {
    /** Funds added to wallet (top-up, transfer received) */
    CREDIT,
    /** Funds deducted from wallet (transfer sent, payment) */
    DEBIT,
    /** Funds reserved for pending payment (legacy, use DEBIT with PENDING status) */
    RESERVE
}
