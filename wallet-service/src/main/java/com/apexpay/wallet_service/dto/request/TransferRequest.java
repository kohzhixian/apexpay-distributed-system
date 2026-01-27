package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for transferring funds between wallets.
 * <p>
 * Transfers funds from the payer's wallet to the receiver's wallet.
 * Both wallets must belong to the specified users and cannot be the same wallet.
 * </p>
 *
 * @param receiverUserId  the user ID of the receiver
 * @param receiverWalletId the wallet ID to receive the funds
 * @param payerWalletId    the wallet ID to deduct funds from
 * @param amount           the amount to transfer (must be positive)
 */
public record TransferRequest(
        @NotBlank(message = ValidationMessages.RECEIVER_USER_ID_REQUIRED)
        String receiverUserId,

        @NotBlank(message = ValidationMessages.RECEIVER_WALLET_ID_REQUIRED)
        UUID receiverWalletId,

        @NotBlank(message = ValidationMessages.PAYER_WALLET_ID_REQUIRED)
        UUID payerWalletId,

        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        BigDecimal amount) {
}
