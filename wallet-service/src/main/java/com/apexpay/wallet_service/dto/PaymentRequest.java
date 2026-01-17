package com.apexpay.wallet_service.dto;

import com.apexpay.common.enums.CurrencyEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Wallet id is required")
        String walletId,

        CurrencyEnum currency,

        @NotBlank(message = "Reference is required")
        String referenceId,

        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be more than 0.")
        BigDecimal amount) {
}
