package com.apexpay.userservice.dto.request;

public record LoginRequest(
        @NotBlank
        String email, String password
) {
}
