package com.apexpay.userservice.dto;

import java.util.UUID;

/**
 * Data transfer object representing the parsed refresh token cookie.
 * The cookie format is "tokenId:rawToken" which gets split into these components.
 *
 * @param refreshTokenId the UUID identifying the token in the database
 * @param rawRefreshToken the unhashed token value for hash verification
 */
public record RefreshTokenCookieText(UUID refreshTokenId, String rawRefreshToken) {
}
