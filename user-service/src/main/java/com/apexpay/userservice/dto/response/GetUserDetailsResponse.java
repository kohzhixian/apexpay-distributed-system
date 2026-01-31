package com.apexpay.userservice.dto.response;

import java.util.UUID;

public record GetUserDetailsResponse(UUID userId, String username) {
}
