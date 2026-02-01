package com.apexpay.userservice.dto.response;

import java.util.UUID;

/**
 * Response DTO for retrieving contact details by recipient email.
 * Used by wallet-service for transfers.
 *
 * @param contactUserId   the user ID of the contact
 * @param contactWalletId the wallet ID of the contact
 * @param contactEmail    the email of the contact
 * @param contactUsername the username of the contact
 */
public record GetContactByEmailResponse(
        UUID contactUserId,
        UUID contactWalletId,
        String contactEmail,
        String contactUsername
) {
}
