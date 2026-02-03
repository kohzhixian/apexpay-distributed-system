package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.Contacts;
import com.apexpay.userservice.entity.Users;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactsRepository extends JpaRepository<@NonNull Contacts, @NonNull UUID> {

    List<Contacts> findByOwnerUserAndIsActiveTrue(Users ownerUser);

    Optional<Contacts> findByOwnerUserAndContactWalletIdAndIsActiveTrue(Users ownerUser, UUID contactWalletId);

    Optional<Contacts> findByIdAndOwnerUser(UUID id, Users ownerUser);

    Optional<Contacts> findByOwnerUserIdAndContactEmailAndIsActiveTrue(UUID ownerUserId, String contactEmail);

    Optional<Contacts> findByOwnerUserAndContactWalletId(Users ownerUser, UUID contactWalletId);
}
