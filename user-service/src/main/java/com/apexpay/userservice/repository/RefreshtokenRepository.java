package com.apexpay.userservice.repository;

import com.apexpay.userservice.entity.RefreshTokens;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing refresh token persistence.
 * Handles CRUD operations for refresh tokens used in JWT authentication.
 */
@Repository
public interface RefreshtokenRepository extends JpaRepository<@NonNull RefreshTokens, @NonNull UUID> {
        @Query("SELECT rt FROM RefreshTokens rt JOIN FETCH rt.user WHERE rt.isRevoked = false AND rt.consumed = false AND rt.expiryDate > CURRENT_TIMESTAMP AND rt.id = :refreshTokenId AND rt.ipAddress = :ipAddress")
        Optional<RefreshTokens> findValidRefreshToken(@Param("refreshTokenId") UUID refreshTokenId,
                        @Param("ipAddress") String ipAddress);

        @Query("SELECT rt FROM RefreshTokens rt JOIN FETCH rt.user WHERE rt.id = :id AND rt.consumed = true")
        Optional<RefreshTokens> findConsumedTokenById(@Param("id") UUID id);

        /**
         * Fetches refresh token with pessimistic lock to prevent race conditions during
         * token rotation.
         * The SELECT FOR UPDATE lock blocks concurrent transactions until this one
         * completes.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT rt FROM RefreshTokens rt JOIN FETCH rt.user WHERE rt.id = :refreshTokenId AND rt.ipAddress = :ipAddress")
        Optional<RefreshTokens> findByIdAndIpAddressForUpdate(@Param("refreshTokenId") UUID refreshTokenId,
                        @Param("ipAddress") String ipAddress);

        @Modifying
        @Query("UPDATE RefreshTokens rt SET rt.isRevoked = true, rt.consumed = true WHERE rt.familyId = :familyId")
        void revokeAllRefreshTokensByFamilyId(@Param("familyId") UUID familyId);

        /**
         * Revokes all tokens in a family EXCEPT the specified token.
         * Used to avoid deadlock when the excluded token is already locked by the
         * calling transaction.
         */
        @Modifying
        @Query("UPDATE RefreshTokens rt SET rt.isRevoked = true, rt.consumed = true WHERE rt.familyId = :familyId AND rt.id != :excludeTokenId")
        void revokeAllRefreshTokensByFamilyIdExcluding(@Param("familyId") UUID familyId,
                        @Param("excludeTokenId") UUID excludeTokenId);

        @Modifying
        @Query("UPDATE RefreshTokens rt SET rt.isRevoked = true WHERE rt.user.id = :userId AND rt.isRevoked = false")
        void revokeAllRefreshTokensByUserId(@Param("userId") UUID userId);
}
