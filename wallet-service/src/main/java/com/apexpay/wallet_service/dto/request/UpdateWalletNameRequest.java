package com.apexpay.wallet_service.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating wallet name.
 *
 * @param walletName the new name for the wallet
 */
public record UpdateWalletNameRequest(
        @NotBlank(message = ValidationMessages.WALLET_NAME_REQUIRED)
        @Size(max = 50, message = ValidationMessages.WALLET_NAME_MAX_LENGTH)
        String walletName) {
}
