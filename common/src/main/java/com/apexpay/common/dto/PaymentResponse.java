package com.apexpay.common.dto;

import com.apexpay.common.enums.PaymentStatusEnum;

import java.util.UUID;

/**
 * Response DTO for payment operations.
 * <p>
 * Returned after processing a payment, indicating the final status and
 * providing a human-readable message about the result.
 * </p>
 *
 * @param paymentId the ID of the processed payment
 * @param status    the final payment status (SUCCESS, FAILED, etc.)
 * @param message   a human-readable message describing the payment result
 */
public record PaymentResponse(UUID paymentId, PaymentStatusEnum status, String message) {
}
