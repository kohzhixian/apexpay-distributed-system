package com.apexpay.wallet_service.dto;

import java.util.UUID;

/**
 * DTO for receiving contact details from user-service.
 * Used during wallet transfers to get recipient information.
 *
 * @param contactUserId   the user ID of the contact
 * @param contactWalletId the wallet ID of the contact
 * @param contactEmail    the email of the contact
 * @param contactUsername the username of the contact
 */
public record ContactDetailsDto(
        UUID contactUserId,
        UUID contactWalletId,
        String contactEmail,
        String contactUsername
) {
}
