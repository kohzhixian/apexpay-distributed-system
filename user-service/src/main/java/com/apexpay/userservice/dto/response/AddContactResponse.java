package com.apexpay.userservice.dto.response;

import java.util.UUID;

public record AddContactResponse(UUID contactId, String message) {
}
