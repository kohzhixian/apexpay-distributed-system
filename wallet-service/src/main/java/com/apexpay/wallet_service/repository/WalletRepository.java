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

    /**
     * Atomically reserves funds in a wallet using optimistic locking.
     * <p>
     * Uses version checking to prevent concurrent modifications.
     * Only succeeds if the wallet has sufficient available balance
     * (balance - reservedBalance >= amount).
     * </p>
     *
     * @param amount         the amount to reserve
     * @param walletId       the wallet ID
     * @param userId         the user ID (for ownership verification)
     * @param currentVersion the expected version for optimistic locking
     * @return 1 if reservation succeeded, 0 if failed (version mismatch or insufficient funds)
     */
    @Modifying
    @Query("UPDATE Wallets w " +
            "SET w.reservedBalance = w.reservedBalance + :amount, " +
            "    w.version = w.version + 1 " +
            "WHERE w.id = :walletId " +
            "AND w.userId = :userId " +
            "AND w.version = :currentVersion " +
            "AND (w.balance - w.reservedBalance) >= :amount")
    int tryReserveFunds(
            @Param("amount") BigDecimal amount,
            @Param("walletId") UUID walletId,
            @Param("userId") UUID userId,
            @Param("currentVersion") Long currentVersion);

    /**
     * Finds all wallets belonging to a specific user.
     *
     * @param userId the user ID
     * @return list of wallets owned by the user
     */
    List<Wallets> findByUserId(UUID userId);

    /**
     * Calculates the total balance across all wallets for a user.
     *
     * @param userId the user ID
     * @return sum of all wallet balances, or 0 if no wallets exist
     */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallets w WHERE w.userId = :userId")
    BigDecimal sumBalanceByUserId(@Param("userId") UUID userId);
}
