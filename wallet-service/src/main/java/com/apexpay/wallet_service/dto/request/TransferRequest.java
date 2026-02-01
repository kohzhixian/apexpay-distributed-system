package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import com.apexpay.common.enums.CurrencyEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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
 * @param payerWalletId  the wallet ID to deduct funds from
 * @param recipientEmail the email address of the recipient
 * @param amount         the amount to transfer (must be positive)
 * @param currency       the currency of the transfer
 */
public record TransferRequest(
        @NotNull(message = ValidationMessages.PAYER_WALLET_ID_REQUIRED)
        UUID payerWalletId,

        @NotBlank(message = ValidationMessages.RECIPIENT_EMAIL_REQUIRED)
        @Email
        String recipientEmail,

        @DecimalMin(value = ValidationMessages.DECIMAL_MIN_AMOUNT, inclusive = false, message = ValidationMessages.AMOUNT_MUST_BE_POSITIVE)
        @NotNull(message = ValidationMessages.AMOUNT_REQUIRED)
        BigDecimal amount,

        @Enumerated(EnumType.STRING)
        CurrencyEnum currency

) {
}
