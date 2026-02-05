package com.apexpay.wallet_service.repository;

import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for wallet transaction persistence operations.
 * <p>
 * Handles CRUD operations for wallet transactions including
 * history queries, status-based lookups, and reference-based searches.
 * </p>
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactions, UUID> {

    /**
     * Finds all transactions for a wallet with pagination.
     *
     * @param walletId the wallet ID to query
     * @param pageable pagination parameters
     * @return list of transactions for the wallet
     */
    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.wallet.id = :walletId")
    List<WalletTransactions> findAllByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Finds a transaction by ID with a specific status.
     *
     * @param walletTransactionId the transaction ID to find
     * @param status              the expected status
     * @return the transaction if found with matching status
     */
    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.id = :walletTransactionId AND wt.status = :status")
    Optional<WalletTransactions> findPendingTransactionById(@Param("walletTransactionId") UUID walletTransactionId, @Param("status") WalletTransactionStatusEnum status);

    /**
     * Finds a transaction by external reference ID and type.
     * Used for idempotency checks on payment reservations.
     *
     * @param referenceId   the external reference ID (e.g., payment ID)
     * @param referenceType the type of reference
     * @return the transaction if found with matching reference
     */
    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.referenceId = :referenceId AND wt.referenceType = :referenceType")
    Optional<WalletTransactions> findByReferenceIdAndReferenceType(@Param("referenceId") String referenceId, @Param("referenceType") com.apexpay.wallet_service.enums.ReferenceTypeEnum referenceType);

    /**
     * Finds recent transactions across all wallets for a user.
     *
     * @param userId   the user ID to query
     * @param pageable pagination parameters (use for limit and sort)
     * @return list of recent transactions for the user
     */
    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.wallet.userId = :userId")
    List<WalletTransactions> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Sums completed transaction amounts by type for a user within a date range.
     *
     * @param userId          the user ID to query
     * @param transactionType the transaction type (CREDIT or DEBIT)
     * @param startDate       the start of the date range (inclusive)
     * @param endDate         the end of the date range (exclusive)
     * @return sum of amounts, or null if no transactions found
     */
    @Query("""
            SELECT COALESCE(SUM(wt.amount), 0)
            FROM WalletTransactions wt
            WHERE wt.wallet.userId = :userId
              AND wt.transactionType = :transactionType
              AND wt.status = com.apexpay.wallet_service.enums.WalletTransactionStatusEnum.COMPLETED
              AND wt.createdDate >= :startDate
              AND wt.createdDate < :endDate
            """)
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("transactionType") com.apexpay.wallet_service.enums.TransactionTypeEnum transactionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
