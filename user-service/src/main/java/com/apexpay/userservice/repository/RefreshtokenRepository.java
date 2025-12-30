package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.RefreshTokens;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing refresh token persistence.
 * Handles CRUD operations for refresh tokens used in JWT authentication.
 */
@Repository
public interface RefreshtokenRepository extends JpaRepository<@NonNull RefreshTokens, @NonNull UUID> {
}
