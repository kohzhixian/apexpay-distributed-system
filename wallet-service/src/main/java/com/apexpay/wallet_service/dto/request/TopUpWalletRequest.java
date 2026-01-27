package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for topping up a wallet.
 * <p>
 * Adds funds to an existing wallet, increasing the available balance.
 * </p>
 *
 * @param amount   the amount to add to the wallet (must be positive)
 * @param walletId the wallet ID to top up
 * @param currency the currency of the top-up amount
 */
public record TopUpWalletRequest(
        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        BigDecimal amount,

        @NotBlank(message = ValidationMessages.WALLET_ID_REQUIRED)
        UUID walletId,

        CurrencyEnum currency) {
}
