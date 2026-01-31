package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new wallet.
 * <p>
 * Used to create a wallet for a user with a name, initial balance, and currency.
 * </p>
 *
 * @param name     user-defined name for the wallet (e.g., "Personal", "Business")
 * @param balance  the initial wallet balance (must be non-negative)
 * @param currency the currency of the wallet (defaults to SGD if not provided)
 */
public record CreateWalletRequest(
        @NotBlank(message = ValidationMessages.WALLET_NAME_REQUIRED)
        @Size(max = 50, message = ValidationMessages.WALLET_NAME_MAX_LENGTH)
        String name,

        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_BALANCE, message = ValidationMessages.BALANCE_MUST_BE_NON_NEGATIVE)
        @NotNull(message = ValidationMessages.BALANCE_REQUIRED)
        BigDecimal balance,

        CurrencyEnum currency) {
}
