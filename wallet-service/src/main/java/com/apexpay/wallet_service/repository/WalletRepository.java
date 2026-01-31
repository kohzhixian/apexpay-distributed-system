package com.apexpay.wallet_service.repository;

import com.apexpay.wallet_service.entity.Wallets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
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
    @Query("SELECT w FROM Wallets w WHERE w.id = :id AND w.userId = :userId")
    Optional<Wallets> findWalletByUserIdAndId(@Param("userId") UUID userId, @Param("id") UUID id);

    @Modifying
    @Query("UPDATE Wallets w " +
            "SET w.reservedBalance = w.reservedBalance + :amount, " +
            "    w.version = w.version + 1 " +
            "WHERE w.id = :walletId " +
            "AND w.userId = :userId " +
            "AND w.version = :currentVersion " +
            "AND (w.balance - w.reservedBalance) >= :amount")
        // returns 1 if succeed and 0 if failed
    int tryReserveFunds(
            @Param("amount") BigDecimal amount,
            @Param("walletId") UUID walletId,
            @Param("userId") UUID userId,
            @Param("currentVersion") Long currentVersion);

    List<Wallets> findByUserId(UUID userId);
}
