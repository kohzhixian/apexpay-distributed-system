package com.apexpay.userservice.dto.response;

import com.apexpay.userservice.dto.ContactDto;

import java.util.List;

/**
 * Response DTO for retrieving user contacts.
 *
 * @param contacts list of the user's active contacts
 */
public record GetContactsResponse(List<ContactDto> contacts) {
}
