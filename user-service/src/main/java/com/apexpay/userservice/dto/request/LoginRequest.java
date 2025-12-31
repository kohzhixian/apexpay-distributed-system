package com.apexpay.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user login authentication.
 *
 * @param email    the user's email address
 * @param password the user's password
 */
public record LoginRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
}
