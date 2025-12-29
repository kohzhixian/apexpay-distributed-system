package com.apexpay.userservice.dto.request;

public record RegisterRequest(
        String email, String username, String password
) {
}
