package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for topping up a wallet.
 * <p>
 * Adds funds to an existing wallet, increasing the available balance.
 * </p>
 *
 * @param amount          the amount to add to the wallet (must be positive)
 * @param walletId        the wallet ID to top up
 * @param currency        the currency of the top-up amount
 * @param paymentMethodId the payment method used for the top-up (updates lastUsedAt for default selection)
 */
public record TopUpWalletRequest(
        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        BigDecimal amount,

        @NotNull(message = ValidationMessages.WALLET_ID_REQUIRED)
        UUID walletId,

        CurrencyEnum currency,

        @NotNull(message = ValidationMessages.PAYMENT_METHOD_ID_REQUIRED)
        UUID paymentMethodId) {
}
