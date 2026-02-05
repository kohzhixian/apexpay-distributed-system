package com.apexpay.userservice.dto.response;

import java.util.UUID;

/**
 * Response DTO for add contact operations.
 *
 * @param contactId the ID of the newly created contact
 * @param message   a success message confirming the operation
 */
public record AddContactResponse(UUID contactId, String message) {
}
