package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.Contacts;
import com.apexpay.userservice.entity.Users;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for contact entity operations.
 * <p>
 * Provides methods for CRUD operations and queries on user contacts.
 * Supports soft-delete pattern with isActive flag filtering.
 * </p>
 */
@Repository
public interface ContactsRepository extends JpaRepository<@NonNull Contacts, @NonNull UUID> {

    /**
     * Finds all active contacts for the specified owner user.
     *
     * @param ownerUser the user who owns the contacts
     * @return list of active contacts
     */
    List<Contacts> findByOwnerUserAndIsActiveTrue(Users ownerUser);

    /**
     * Finds an active contact by owner user and contact's wallet ID.
     *
     * @param ownerUser       the user who owns the contact
     * @param contactWalletId the wallet ID of the contact
     * @return optional containing the contact if found
     */
    Optional<Contacts> findByOwnerUserAndContactWalletIdAndIsActiveTrue(Users ownerUser, UUID contactWalletId);

    /**
     * Finds a contact by ID and owner user (regardless of active status).
     *
     * @param id        the contact ID
     * @param ownerUser the user who owns the contact
     * @return optional containing the contact if found
     */
    Optional<Contacts> findByIdAndOwnerUser(UUID id, Users ownerUser);

    /**
     * Finds an active contact by owner user ID and contact's email.
     *
     * @param ownerUserId  the ID of the user who owns the contact
     * @param contactEmail the email address of the contact
     * @return optional containing the contact if found
     */
    Optional<Contacts> findByOwnerUserIdAndContactEmailAndIsActiveTrue(UUID ownerUserId, String contactEmail);

    /**
     * Finds a contact by owner user and contact's wallet ID (regardless of active status).
     * Used for soft-delete reactivation checks.
     *
     * @param ownerUser       the user who owns the contact
     * @param contactWalletId the wallet ID of the contact
     * @return optional containing the contact if found
     */
    Optional<Contacts> findByOwnerUserAndContactWalletId(Users ownerUser, UUID contactWalletId);
}
