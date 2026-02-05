package com.apexpay.userservice.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.userservice.dto.request.AddContactRequest;
import com.apexpay.userservice.dto.response.AddContactResponse;
import com.apexpay.userservice.dto.response.DeleteContactResponse;
import com.apexpay.userservice.dto.response.GetContactByEmailResponse;
import com.apexpay.userservice.dto.response.GetContactsResponse;
import com.apexpay.userservice.service.ContactsService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing user contacts.
 * <p>
 * Provides endpoints for adding, retrieving, and deleting contacts.
 * All endpoints require authentication via the X-USER-EMAIL or X-USER-ID header.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactsController {
    private final ContactsService contactsService;

    /**
     * Constructs a new ContactsController with the required service.
     *
     * @param contactsService the contacts service for business logic
     */
    public ContactsController(ContactsService contactsService) {
        this.contactsService = contactsService;
    }

    /**
     * Adds a new contact for the authenticated user.
     *
     * @param email   the authenticated user's email from the X-USER-EMAIL header
     * @param request the contact details including email and wallet ID
     * @return 201 Created with the new contact's ID and success message
     */
    @PostMapping
    public ResponseEntity<@NonNull AddContactResponse> addContact(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email,
            @Valid @RequestBody AddContactRequest request
    ) {
        AddContactResponse response = contactsService.addContact(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all active contacts for the authenticated user.
     *
     * @param email the authenticated user's email from the X-USER-EMAIL header
     * @return 200 OK with list of contacts
     */
    @GetMapping
    public ResponseEntity<@NonNull GetContactsResponse> getContacts(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email
    ) {
        GetContactsResponse response = contactsService.getContacts(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a contact for the authenticated user.
     *
     * @param email     the authenticated user's email from the X-USER-EMAIL header
     * @param contactId the ID of the contact to delete
     * @return 200 OK with success message
     */
    @DeleteMapping("/{contactId}")
    public ResponseEntity<@NonNull DeleteContactResponse> deleteContact(
            @RequestHeader(HttpHeaders.X_USER_EMAIL) String email,
            @PathVariable UUID contactId
    ) {
        DeleteContactResponse response = contactsService.deleteContact(email, contactId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a contact by the recipient's email address.
     * <p>
     * Used for wallet transfers to lookup contact details by email.
     * </p>
     *
     * @param ownerId        the authenticated user's ID from the X-USER-ID header
     * @param recipientEmail the email address of the recipient to find
     * @return 200 OK with contact details including user ID, wallet ID, email, and username
     */
    @GetMapping("/recipient")
    public ResponseEntity<@NonNull GetContactByEmailResponse> getContactByRecipientEmail(
            @RequestHeader(HttpHeaders.X_USER_ID) String ownerId,
            @RequestParam String recipientEmail
    ) {
        GetContactByEmailResponse response = contactsService.getContactByRecipientEmail(ownerId, recipientEmail);
        return ResponseEntity.ok(response);
    }
}
