package com.apexpay.userservice.dto;

import com.apexpay.userservice.entity.RefreshTokens;

/**
 * Holds both the refresh token entity (for database storage) and the raw token (for client cookie).
 * Separates the hashed token stored in DB from the raw token sent to the client.
 *
 * @param entity       the refresh token entity containing the hashed token for database persistence
 * @param refreshToken the raw (unhashed) refresh token to be sent to the client via HTTP-only cookie
 */
public record RefreshTokenObj(RefreshTokens entity, String refreshToken) {
}
