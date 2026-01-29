package com.apexpay.payment_service.dto;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for initiating a payment.
 * <p>
 * Creates a new payment record in INITIATED status. The clientRequestId
 * ensures idempotency - if a payment with the same clientRequestId already
 * exists for the user, the existing payment is returned instead of creating
 * a duplicate.
 * </p>
 *
 * @param amount          the payment amount (must be positive)
 * @param walletId        the wallet ID to charge for the payment
 * @param currency        the currency of the payment
 * @param clientRequestId a unique identifier provided by the client for idempotency
 * @param provider        the payment provider name (e.g., "STRIPE", "MOCK")
 */
public record InitiatePaymentRequest(
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        BigDecimal amount,

        @NotNull(message = ValidationMessages.WALLET_ID_REQUIRED)
        UUID walletId,

        @NotNull(message = ValidationMessages.CURRENCY_REQUIRED)
        CurrencyEnum currency,

        @NotBlank(message = ValidationMessages.CLIENT_REQUEST_ID_REQUIRED)
        String clientRequestId,

        @NotBlank(message = ValidationMessages.PROVIDER_REQUIRED)
        String provider,

        @NotBlank(message = ValidationMessages.EXTERNAL_TRANSACTION_ID_REQUIRED)
        String externalTransactionId

) {
}
