package com.apexpay.common.enums;

/**
 * Enumeration of payment status values.
 * <p>
 * Represents the lifecycle states of a payment transaction:
 * <ul>
 * <li>INITIATED - Payment record created, awaiting processing</li>
 * <li>PENDING - Funds reserved, payment provider charge in progress</li>
 * <li>SUCCESS - Payment successfully completed</li>
 * <li>FAILED - Payment failed (provider declined, insufficient funds, etc.)</li>
 * <li>REFUNDED - Payment was successfully refunded</li>
 * </ul>
 * </p>
 */
public enum PaymentStatusEnum {
    /** Payment record created, awaiting processing */
    INITIATED,
    /** Payment successfully completed */
    SUCCESS,
    /** Funds reserved, payment provider charge in progress */
    PENDING,
    /** Payment failed (provider declined, insufficient funds, etc.) */
    FAILED,
    /** Payment was successfully refunded */
    REFUNDED
}
