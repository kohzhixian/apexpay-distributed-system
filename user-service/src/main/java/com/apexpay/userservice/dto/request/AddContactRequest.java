package com.apexpay.userservice.dto.request;

import com.apexpay.common.constants.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddContactRequest(
        @NotBlank(message = ValidationMessages.CONTACT_EMAIL_REQUIRED)
        @Email(message = ValidationMessages.CONTACT_EMAIL_INVALID)
        String contactEmail,

        @NotNull(message = ValidationMessages.WALLET_ID_REQUIRED)
        UUID walletId
) {
}
