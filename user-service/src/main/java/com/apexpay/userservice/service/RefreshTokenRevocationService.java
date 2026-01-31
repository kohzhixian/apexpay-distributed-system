package com.apexpay.userservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import com.apexpay.userservice.repository.RefreshtokenRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * Service for revoking refresh token families in a separate transaction.
 * <p>
 * This service exists to enable {@code REQUIRES_NEW} transaction propagation,
 * which only works when called from a different Spring bean. Used for cascade
 * revocation when token reuse is detected (potential security breach).
 * </p>
 */
@Service
public class RefreshTokenRevocationService {
    private final RefreshtokenRepository refreshtokenRepository;

    public RefreshTokenRevocationService(
            RefreshtokenRepository refreshtokenRepository) {
        this.refreshtokenRepository = refreshtokenRepository;
    }

    /**
     * Revokes all tokens in a token family.
     * Executes in a new transaction to ensure revocation commits even if
     * the calling transaction rolls back.
     *
     * @param familyId the UUID of the token family to revoke
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeTokenFamily(UUID familyId) {
        refreshtokenRepository.revokeAllRefreshTokensByFamilyId(familyId);
    }

    /**
     * Revokes all tokens in a family except the specified token.
     * Use this when the excluded token is already locked by the calling transaction
     * to avoid deadlock (REQUIRES_NEW creates separate connection that would block
     * on the lock).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeTokenFamilyExcluding(UUID familyId, UUID excludeTokenId) {
        refreshtokenRepository.revokeAllRefreshTokensByFamilyIdExcluding(familyId, excludeTokenId);
    }
}
