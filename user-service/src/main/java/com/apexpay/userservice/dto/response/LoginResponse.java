package com.apexpay.userservice.dto.response;

/**
 * Response payload for successful login operations.
 *
 * @param message confirmation message for the login result
 */
public record LoginResponse(
        String message
) {
}
