package com.apexpay.userservice.dto;

import java.util.UUID;

/**
 * Data transfer object for contact information.
 *
 * @param contactId the unique identifier of the contact
 * @param username  the username of the contact
 * @param email     the email address of the contact
 */
public record ContactDto(
        UUID contactId,
        String username,
        String email
) {
}
