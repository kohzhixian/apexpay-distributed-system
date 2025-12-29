package com.apexpay.userservice.dto.response;

public record RegisterResponse(
        String accessToken, String refreshToken, String message
) {
}
