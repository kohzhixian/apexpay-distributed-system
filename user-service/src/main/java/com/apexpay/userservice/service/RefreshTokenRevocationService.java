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
}
