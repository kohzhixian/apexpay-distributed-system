package com.apexpay.common.dto;

import com.apexpay.common.enums.CurrencyEnum;
import com.apexpay.common.enums.PaymentStatusEnum;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment operations.
 * <p>
 * Returned after processing a payment, indicating the final status and
 * providing a human-readable message about the result.
 * </p>
 *
 * @param paymentId the ID of the processed payment
 * @param status    the final payment status (INITIATED, SUCCESS, PENDING, FAILED, REFUNDED)
 * @param message   a human-readable message describing the payment result
 * @param amount    the payment amount
 * @param currency  the currency of the payment
 * @param createdAt timestamp when the payment was created
 * @param updatedAt timestamp when the payment was last updated
 */
public record PaymentResponse(
        UUID paymentId,
        PaymentStatusEnum status,
        String message,
        BigDecimal amount,
        CurrencyEnum currency,
        Instant createdAt,
        Instant updatedAt
) {
}
