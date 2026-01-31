package com.apexpay.wallet_service.enums;

/**
 * Enumeration of reference types for wallet transactions.
 * <p>
 * Links wallet transactions to their originating operations, allowing
 * traceability and categorization of financial movements.
 * </p>
 */
public enum ReferenceTypeEnum {
    /** Transaction related to a payment */
    PAYMENT,
    /** Transaction related to an order */
    ORDER,
    /** Transaction related to a refund */
    REFUND,
    /** Transaction from an administrative adjustment */
    ADMIN_ADJUSTMENT,
    /** Transaction related to a wallet-to-wallet transfer */
    TRANSFER,
    /** Transaction related to a wallet top-up */
    TOPUP
}
