package com.apexpay.wallet_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Receiver user id is required.")
        String receiverUserId,

        @NotBlank(message = "Receiver wallet id is required.")
        String receiverWalletId,

        @NotBlank(message = "Payer wallet id is required.")
        String payerWalletId,

        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be more than 0.")
        BigDecimal amount) {
}
