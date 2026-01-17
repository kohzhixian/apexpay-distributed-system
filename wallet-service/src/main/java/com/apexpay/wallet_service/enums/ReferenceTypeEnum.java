package com.apexpay.wallet_service.enums;

/**
 * Transaction reference types for wallet operations.
 * Links wallet transactions to their originating operations.
 */
public enum ReferenceTypeEnum {
    PAYMENT,
    ORDER,
    REFUND,
    ADMIN_ADJUSTMENT,
    TRANSFER
}
