package com.apexpay.wallet_service.helper;

import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.repository.WalletTransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component responsible for saving wallet transactions in a new transaction.
 * <p>
 * This class exists to solve the problem of retrying database saves after a
 * DataIntegrityViolationException. When such an exception occurs, the Hibernate
 * Session becomes corrupted and the transaction is marked for rollback. By using
 * {@code REQUIRES_NEW} propagation, each save attempt gets its own independent
 * transaction, allowing retry logic to work correctly.
 * </p>
 */
@Component
public class WalletTransactionSaver {

    private final WalletTransactionRepository walletTransactionRepository;

    public WalletTransactionSaver(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /**
     * Saves a wallet transaction in a new, independent transaction.
     * <p>
     * Uses {@code REQUIRES_NEW} propagation to ensure this operation runs in its
     * own transaction, isolated from the calling transaction. This allows the caller
     * to catch DataIntegrityViolationException and retry without corrupting the
     * parent transaction's Hibernate Session.
     * </p>
     *
     * @param transaction the wallet transaction to save
     * @return the saved wallet transaction with generated ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactions saveInNewTransaction(WalletTransactions transaction) {
        return walletTransactionRepository.save(transaction);
    }
}
