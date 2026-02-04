package com.apexpay.userservice.service;

import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.userservice.dto.ContactDto;
import com.apexpay.userservice.dto.request.AddContactRequest;
import com.apexpay.userservice.dto.response.AddContactResponse;
import com.apexpay.userservice.dto.response.DeleteContactResponse;
import com.apexpay.userservice.dto.response.GetContactByEmailResponse;
import com.apexpay.userservice.dto.response.GetContactsResponse;
import com.apexpay.userservice.entity.Contacts;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.ContactsRepository;
import com.apexpay.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContactsService {
    private final ContactsRepository contactsRepository;
    private final UserRepository userRepository;

    public ContactsService(ContactsRepository contactsRepository, UserRepository userRepository) {
        this.contactsRepository = contactsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AddContactResponse addContact(String ownerEmail, AddContactRequest request) {
        Users ownerUser = userRepository.findByEmail(ownerEmail);
        if (ownerUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Owner user not found.");
        }

        Users contactUser = userRepository.findByEmail(request.contactEmail());
        if (contactUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Contact user not found.");
        }

        if (ownerUser.getId().equals(contactUser.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_CONTACT, "Cannot add yourself as a contact.");
        }

        // Check if contact with same email already exists and is active (prevents
        // ambiguous lookups)
        boolean emailContactExists = contactsRepository
                .findByOwnerUserIdAndContactEmailAndIsActiveTrue(ownerUser.getId(), request.contactEmail())
                .isPresent();

        if (emailContactExists) {
            throw new BusinessException(ErrorCode.CONTACT_ALREADY_EXISTS,
                    "This person is already in your contacts.");
        }

        // Check if contact exists (active or inactive) - handles soft-delete
        // reactivation
        Optional<Contacts> existingContact = contactsRepository
                .findByOwnerUserAndContactWalletId(ownerUser, request.walletId());

        if (existingContact.isPresent()) {
            Contacts contact = existingContact.get();
            if (contact.getIsActive()) {
                throw new BusinessException(ErrorCode.CONTACT_ALREADY_EXISTS, "This contact already exists.");
            }
            // Reactivate soft-deleted contact
            contact.setIsActive(true);
            contact.setContactUser(contactUser);
            contact.setContactEmail(contactUser.getEmail());
            contact.setContactUsername(contactUser.getUsername());
            contactsRepository.save(contact);
            return new AddContactResponse(contact.getId(), "Contact added successfully.");
        }

        Contacts contact = Contacts.builder()
                .ownerUser(ownerUser)
                .contactUser(contactUser)
                .contactWalletId(request.walletId())
                .contactEmail(contactUser.getEmail())
                .contactUsername(contactUser.getUsername())
                .build();

        Contacts savedContact = contactsRepository.save(contact);

        return new AddContactResponse(savedContact.getId(), "Contact added successfully.");
    }

    @Transactional(readOnly = true)
    public GetContactsResponse getContacts(String ownerEmail) {
        Users ownerUser = userRepository.findByEmail(ownerEmail);
        if (ownerUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }

        List<Contacts> contacts = contactsRepository.findByOwnerUserAndIsActiveTrue(ownerUser);

        List<ContactDto> contactDtos = contacts.stream()
                .map(contact -> new ContactDto(
                        contact.getId(),
                        contact.getContactUsername(),
                        contact.getContactEmail()))
                .toList();

        return new GetContactsResponse(contactDtos);
    }

    @Transactional
    public DeleteContactResponse deleteContact(String ownerEmail, UUID contactId) {
        Users ownerUser = userRepository.findByEmail(ownerEmail);
        if (ownerUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }

        Contacts contact = contactsRepository.findByIdAndOwnerUser(contactId, ownerUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_NOT_FOUND, "Contact not found."));

        contact.setIsActive(false);
        contactsRepository.save(contact);

        return new DeleteContactResponse("Contact deleted successfully.");
    }

    @Transactional(readOnly = true)
    public GetContactByEmailResponse getContactByRecipientEmail(String ownerId, String recipientEmail) {
        UUID ownerUuid = UUID.fromString(ownerId);

        Contacts contact = contactsRepository
                .findByOwnerUserIdAndContactEmailAndIsActiveTrue(ownerUuid, recipientEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_NOT_FOUND,
                        "Contact not found. Please add this recipient to your contacts first."));

        return new GetContactByEmailResponse(
                contact.getContactUser().getId(),
                contact.getContactWalletId(),
                contact.getContactEmail(),
                contact.getContactUsername());
    }
}
