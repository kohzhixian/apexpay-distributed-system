package com.apexpay.payment_service.dto;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for processing a payment.
 * <p>
 * Contains the payment method ID referencing a saved payment method.
 * The backend retrieves the provider token from the saved payment method
 * and updates the lastUsedAt timestamp on successful payment.
 * </p>
 *
 * @param paymentMethodId the ID of the saved payment method to charge
 */
public record ProcessPaymentRequest(
        @NotNull(message = ValidationMessages.PAYMENT_METHOD_ID_REQUIRED)
        UUID paymentMethodId
) {
}
