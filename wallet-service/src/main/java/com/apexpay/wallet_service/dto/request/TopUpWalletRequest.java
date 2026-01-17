package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpWalletRequest(
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be more than 0.")
        @NotNull(message = "Amount is required.")
        BigDecimal amount,

        @NotBlank(message = "Wallet id is required.")
        String walletId,

        CurrencyEnum currency) {
}
