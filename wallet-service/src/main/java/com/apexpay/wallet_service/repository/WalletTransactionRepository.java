package com.apexpay.wallet_service.repository;

import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactions, UUID> {
    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.wallet.id = :walletId")
    List<WalletTransactions> findAllByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    @Query("SELECT wt FROM WalletTransactions wt WHERE wt.id = :walletTransactionId AND wt.status = :status")
    Optional<WalletTransactions> findPendingTransactionById(@Param("walletTransactionId") UUID walletTransactionId, @Param("status") WalletTransactionStatusEnum status);
}
