package com.apexpay.userservice.dto.request;

import java.time.Instant;

/**
 * Request DTO for refresh token validation and renewal.
 * Contains the data needed to verify and rotate a refresh token.
 *
 * @param userId             the user's unique identifier
 * @param hashedRefreshToken the hashed refresh token from the database
 * @param expiryDate         the token's expiration timestamp
 * @param ipAddress          the client's IP address for security validation
 */
public record RefreshRequest(
        String userId, String hashedRefreshToken, Instant expiryDate, String ipAddress) {
}
