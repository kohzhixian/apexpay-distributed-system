package com.apexpay.userservice.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.userservice.dto.request.AddContactRequest;
import com.apexpay.userservice.dto.response.AddContactResponse;
import com.apexpay.userservice.dto.response.DeleteContactResponse;
import com.apexpay.userservice.dto.response.GetContactsResponse;
import com.apexpay.userservice.service.ContactsService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactsController {
    private final ContactsService contactsService;

    public ContactsController(ContactsService contactsService) {
        this.contactsService = contactsService;
    }

    @PostMapping
    public ResponseEntity<@NonNull AddContactResponse> addContact(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email,
            @Valid @RequestBody AddContactRequest request
    ) {
        AddContactResponse response = contactsService.addContact(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<@NonNull GetContactsResponse> getContacts(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email
    ) {
        GetContactsResponse response = contactsService.getContacts(email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<@NonNull DeleteContactResponse> deleteContact(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email,
            @PathVariable UUID contactId
    ) {
        DeleteContactResponse response = contactsService.deleteContact(email, contactId);
        return ResponseEntity.ok(response);
    }
}
