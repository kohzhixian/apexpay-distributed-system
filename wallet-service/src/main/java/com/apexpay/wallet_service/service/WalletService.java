package com.apexpay.wallet_service.service;

import com.apexpay.common.enums.CurrencyEnum;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.wallet_service.dto.*;
import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.entity.Wallets;
import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.repository.WalletTransactionRepository;
import com.apexpay.wallet_service.repository.WalletsRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service handling wallet operations including creation, top-up, transfers, and
 * payments.
 * Uses optimistic locking with retry mechanism to handle concurrent updates.
 */
@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletsRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(
            WalletsRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    /**
     * Creates a new wallet for the specified user with initial balance.
     */
    @Transactional
    public CreateWalletResponse createWallet(CreateWalletRequest request, String userId) {
        UUID userUuid = parseUserId(userId);

        Wallets newWallet = Wallets.builder()
                .balance(request.balance())
                .userId(userUuid)
                .currency(resolveCurrency(request.currency()))
                .build();

        walletRepository.save(newWallet);
        logger.info("Wallet created. WalletId: {}, UserId: {}", newWallet.getId(), userUuid);
        return new CreateWalletResponse("Wallet created successfully.");
    }

    /**
     * Adds funds to an existing wallet.
     * Retries up to 3 times on concurrent modification conflicts.
     */
    @Transactional
    @Retryable(retryFor = { OptimisticLockException.class,
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TopUpWalletResponse topUpWallet(TopUpWalletRequest request, String userId) {
        Wallets existingWallet = getWalletByUserIdAndId(userId, request.walletId());

        // Calculate new balance of wallet
        BigDecimal newBalance = existingWallet.getBalance().add(request.amount());
        existingWallet.setBalance(newBalance);
        walletRepository.save(existingWallet);

        createTransaction(existingWallet, request.amount(), TransactionTypeEnum.CREDIT, "Top up wallet");
        logger.info("Top up successful. WalletId: {}, Amount: {}", existingWallet.getId(), request.amount());
        return new TopUpWalletResponse("Wallet top up successfully.");
    }

    /**
     * Recovery handler when top-up retries are exhausted.
     */
    @Recover
    public TopUpWalletResponse recoverTopUp(OptimisticLockException ex, TopUpWalletRequest request, String userId) {
        logger.error("Topup failed after retries. UserId: {} , WalletID: {}", userId, request.walletId());
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    /**
     * Transfers funds from payer wallet to receiver wallet.
     * Retries up to 3 times on concurrent modification conflicts.
     */
    @Transactional
    @Retryable(retryFor = { OptimisticLockException.class,
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TransferResponse transfer(TransferRequest request, String payerUserId) {
        validateNotSameWallet(request.payerWalletId(), request.receiverWalletId());

        // get payer wallet
        Wallets payerWallet = getWalletByUserIdAndId(payerUserId, request.payerWalletId());

        // get receiver wallet
        Wallets receiverWallet = getWalletByUserIdAndId(request.receiverUserId(), request.receiverWalletId());

        // ! TODO: Check if currency matches for both wallets

        // check if payer wallet has enough balance for transfer
        validateSufficientBalance(payerWallet, request.amount());

        // deduct amount from payer wallet
        BigDecimal payerBalance = payerWallet.getBalance().subtract(request.amount());
        payerWallet.setBalance(payerBalance);
        walletRepository.save(payerWallet);

        // add a new wallet transaction for payer wallet
        createTransaction(payerWallet, request.amount(), TransactionTypeEnum.DEBIT, "Transfer money.",
                request.receiverWalletId(), ReferenceTypeEnum.TRANSFER);

        // Add amount to receiver wallet
        BigDecimal receiverBalance = receiverWallet.getBalance().add(request.amount());
        receiverWallet.setBalance(receiverBalance);
        walletRepository.save(receiverWallet);

        // add a new wallet transaction for receiver wallet
        createTransaction(receiverWallet, request.amount(), TransactionTypeEnum.CREDIT, "Received money from transfer.",
                request.payerWalletId(), ReferenceTypeEnum.TRANSFER);

        logger.info("Transfer successful. From: {}, To: {}, Amount: {}",
                payerWallet.getId(), receiverWallet.getId(), request.amount());
        return new TransferResponse("Transfer made successfully.");
    }

    /**
     * Recovery handler when transfer retries are exhausted.
     */
    @Recover
    public TransferResponse recoverTransfer(OptimisticLockException e, TransferRequest request, String payerUserId) {
        logger.error("Transfer failed after retries. PayerUserId: {}", payerUserId);
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    /**
     * Processes a payment from the user's wallet.
     * Retries up to 3 times on concurrent modification conflicts.
     */
    @Transactional
    @Retryable(retryFor = { OptimisticLockException.class,
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public PaymentResponse payment(PaymentRequest request, String userId) {
        Wallets existingWallet = getWalletByUserIdAndId(userId, request.walletId());

        // check if user wallet has enough balance to make payment
        validateSufficientBalance(existingWallet, request.amount());

        // calculate wallet new balance
        BigDecimal newBalance = existingWallet.getBalance().subtract(request.amount());
        existingWallet.setBalance(newBalance);
        walletRepository.save(existingWallet);

        // Create new wallet transaction
        createTransaction(existingWallet, request.amount(), TransactionTypeEnum.DEBIT, "Make payment",
                request.referenceId(), ReferenceTypeEnum.PAYMENT);

        logger.info("Payment successful. WalletId: {}, Amount: {}", existingWallet.getId(), request.amount());
        return new PaymentResponse("Payment made successfully.");
    }

    /**
     * Recovery handler when payment retries are exhausted.
     */
    @Recover
    public PaymentResponse recoverPayment(OptimisticLockException e, PaymentRequest request, String userId) {
        logger.error("Payment failed after retries. UserId: {}, WalletId: {}", userId, request.walletId());
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    private Wallets getWalletByUserIdAndId(String userId, String walletId) {
        UUID userUuid = parseUserId(userId);
        UUID walletUuid = parseWalletId(walletId);

        return walletRepository.findWalletByUserIdAndId(userUuid, walletUuid)
                .orElseThrow(() -> {
                    return new BusinessException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found.");
                });
    }

    private void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
            String description) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .type(transactionType)
                .description(description)
                .build();

        walletTransactionRepository.save(newWalletTransaction);
    }

    private void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
            String description, String referenceId, ReferenceTypeEnum referenceType) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .type(transactionType)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        walletTransactionRepository.save(newWalletTransaction);
    }

    private void validateSufficientBalance(Wallets wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient balance.");
        }
    }

    private void validateNotSameWallet(String wallet1Id, String wallet2Id) {
        if (wallet1Id.equals(wallet2Id)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER, "Cannot transfer to the same wallet.");
        }
    }

    private UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "User id is required.");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid User id format.");
        }
    }

    private UUID parseWalletId(String walletId) {
        if (walletId == null || walletId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Wallet id is required.");
        }
        try {
            return UUID.fromString(walletId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid Wallet id format.");
        }
    }

    private CurrencyEnum resolveCurrency(CurrencyEnum currency) {
        return currency != null ? currency : CurrencyEnum.SGD;
    }
}
