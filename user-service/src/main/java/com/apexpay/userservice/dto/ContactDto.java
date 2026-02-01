package com.apexpay.userservice.dto;

import java.util.UUID;

public record ContactDto(
        UUID contactId,
        String username,
        String email
) {
}
