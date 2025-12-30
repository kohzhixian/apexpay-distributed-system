package com.apexpay.userservice.dto.response;

/**
 * Response payload for successful registration operations.
 *
 * @param message confirmation message for the registration result
 */
public record RegisterResponse(
        String message
) {
}
