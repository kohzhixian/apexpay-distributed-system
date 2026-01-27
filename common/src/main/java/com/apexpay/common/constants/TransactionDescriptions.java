package com.apexpay.common.constants;

/**
 * Centralized transaction descriptions used in wallet transactions.
 */
public final class TransactionDescriptions {
    private TransactionDescriptions() {} // Prevent instantiation

    public static final String TOP_UP_WALLET = "Top up wallet";
    public static final String TRANSFER_DEBIT = "Transfer money.";
    public static final String TRANSFER_CREDIT = "Received money from transfer.";
    public static final String RESERVE_FUNDS = "Reserve funds for payment.";
    public static final String PAYMENT_FOR_ORDER = "Payment for order";
}
