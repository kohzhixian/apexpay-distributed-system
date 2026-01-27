package com.apexpay.common.dto;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for payment operations.
 * <p>
 * Contains the necessary information to initiate a payment transaction,
 * including the wallet to charge, amount, currency, and a reference identifier.
 * </p>
 *
 * @param walletId   the wallet ID to charge for the payment
 * @param currency   the currency of the payment (optional, may default to wallet currency)
 * @param referenceId a unique reference identifier for the payment (e.g., order ID)
 * @param amount     the payment amount (must be positive)
 */
public record PaymentRequest(
        @NotNull(message = ValidationMessages.WALLET_ID_REQUIRED)
        UUID walletId,

        CurrencyEnum currency,

        @NotBlank(message = ValidationMessages.REFERENCE_REQUIRED)
        String referenceId,

        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        BigDecimal amount) {
}
