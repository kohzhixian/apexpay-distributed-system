package com.apexpay.wallet_service.dto;

import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateWalletRequest(
        @DecimalMin(value = "0.0", message = "balance must be at least zero")
        @NotNull(message = "Balance is required.")
        BigDecimal balance,

        CurrencyEnum currency) {
}
