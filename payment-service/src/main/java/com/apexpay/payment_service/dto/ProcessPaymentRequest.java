package com.apexpay.payment_service.dto;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for processing a payment.
 * <p>
 * Contains the payment method token (e.g., card token from Stripe) that
 * will be charged by the payment provider.
 * </p>
 *
 * @param paymentMethodToken the token representing the payment method to charge
 */
public record ProcessPaymentRequest(
        @NotBlank(message = ValidationMessages.PAYMENT_METHOD_TOKEN_REQUIRED)
        String paymentMethodToken
) {
}
