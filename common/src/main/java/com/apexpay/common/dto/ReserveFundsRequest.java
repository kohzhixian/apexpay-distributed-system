package com.apexpay.common.dto;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for reserving funds in a wallet.
 * <p>
 * Used in the first phase of the two-phase commit pattern for payment processing.
 * Reserves the specified amount from the wallet's available balance, moving it
 * to reserved balance until the payment is confirmed or cancelled.
 * </p>
 *
 * @param amount   the amount to reserve (must be positive)
 * @param currency the currency of the reservation
 * @param paymentId the payment ID this reservation is associated with
 */
public record ReserveFundsRequest(
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        BigDecimal amount,

        @NotNull(message = ValidationMessages.CURRENCY_REQUIRED)
        CurrencyEnum currency,

        @NotNull(message = ValidationMessages.PAYMENT_ID_REQUIRED)
        UUID paymentId
) {
}
