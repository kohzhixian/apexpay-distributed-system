package com.apexpay.wallet_service.repository;

import com.apexpay.wallet_service.entity.Wallets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for wallet entity operations.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallets, UUID> {

    /**
     * Finds a wallet by user ID and wallet ID.
     * Ensures wallet belongs to the specified user for authorization.
     */
    @Query("Select w FROM Wallets w WHERE w.id = :id AND w.userId = :userId")
    Optional<Wallets> findWalletByUserIdAndId(@Param("userId") UUID userId, @Param("id") UUID id);
}
