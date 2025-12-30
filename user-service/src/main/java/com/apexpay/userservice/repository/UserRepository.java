package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.Users;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing user persistence.
 * Provides methods to query users by username and email for authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<@NonNull Users, @NonNull UUID> {
    
    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return the user if found, null otherwise
     */
    Users findByUsername(String username);

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return the user if found, null otherwise
     */
    Users findByEmail(String email);
}
