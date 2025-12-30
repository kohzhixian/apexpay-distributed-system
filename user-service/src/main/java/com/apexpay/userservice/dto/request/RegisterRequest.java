package com.apexpay.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for new user registration.
 *
 * @param email    the user's email address (must be valid format)
 * @param username the desired username (3-20 chars, alphanumeric and underscores only)
 * @param password the user's password (minimum 8 characters)
 */
public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 3, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String username,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
