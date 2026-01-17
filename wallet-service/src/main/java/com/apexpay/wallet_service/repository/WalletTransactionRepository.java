package com.apexpay.wallet_service.repository;

import com.apexpay.wallet_service.entity.WalletTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactions, UUID> {
}
