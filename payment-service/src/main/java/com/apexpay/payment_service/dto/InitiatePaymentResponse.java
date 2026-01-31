package com.apexpay.payment_service.dto;

import java.util.UUID;

/**
 * Response DTO for payment initiation.
 * <p>
 * Returned after creating or retrieving an existing payment. The version
 * field is used for optimistic locking in subsequent payment processing
 * operations.
 * </p>
 *
 * @param message      a human-readable message indicating whether a new payment
 *                     was created or an existing one was returned
 * @param paymentId    the ID of the payment (newly created or existing)
 * @param version      the current version of the payment entity (for optimistic locking)
 * @param isNewPayment true if a new payment was created, false if returning existing (idempotent)
 */
public record InitiatePaymentResponse(String message, UUID paymentId, Long version, boolean isNewPayment) {
}
