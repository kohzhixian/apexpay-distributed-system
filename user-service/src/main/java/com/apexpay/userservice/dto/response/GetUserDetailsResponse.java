package com.apexpay.userservice.dto.response;

import java.util.UUID;

/**
 * Response DTO for retrieving user profile details.
 *
 * @param userId   the unique identifier of the user
 * @param username the display name of the user
 */
public record GetUserDetailsResponse(UUID userId, String username) {
}
