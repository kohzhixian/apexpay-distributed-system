package com.apexpay.userservice.dto.response;

import com.apexpay.userservice.dto.ContactDto;

import java.util.List;

public record GetContactsResponse(List<ContactDto> contacts) {
}
