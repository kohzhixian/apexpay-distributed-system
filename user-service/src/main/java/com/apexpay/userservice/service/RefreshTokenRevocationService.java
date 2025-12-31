package com.apexpay.userservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import com.apexpay.userservice.repository.RefreshtokenRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenRevocationService {
    private final RefreshtokenRepository refreshtokenRepository;

    public RefreshTokenRevocationService(
            RefreshtokenRepository refreshtokenRepository) {
        this.refreshtokenRepository = refreshtokenRepository;
    }

    // requires new only work from a different spring bean
    // hence the creation of this class
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
