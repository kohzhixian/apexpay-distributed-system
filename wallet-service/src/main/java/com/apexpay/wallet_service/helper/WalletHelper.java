package com.apexpay.wallet_service.helper;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.enums.CurrencyEnum;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.entity.Wallets;
import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;
import com.apexpay.wallet_service.repository.WalletRepository;
import com.apexpay.wallet_service.repository.WalletTransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Helper component for common wallet operations.
 * <p>
 * Provides reusable methods for wallet retrieval, validation,
 * and transaction creation used across the wallet service.
 * </p>
 */
@Component
public class WalletHelper {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TransactionReferenceGenerator transactionReferenceGenerator;

    public WalletHelper(WalletRepository walletRepository,
                        WalletTransactionRepository walletTransactionRepository,
                        TransactionReferenceGenerator transactionReferenceGenerator) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.transactionReferenceGenerator = transactionReferenceGenerator;
    }

    /**
     * Retrieves a wallet by user ID and wallet ID.
     *
     * @param userId   the user ID who owns the wallet
     * @param walletId the wallet ID to retrieve
     * @return the wallet entity
     * @throws BusinessException if wallet not found
     */
    public Wallets getWalletByUserIdAndId(String userId, UUID walletId) {
        UUID userUuid = parseUserId(userId);

        return walletRepository.findWalletByUserIdAndId(userUuid, walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND, ErrorMessages.WALLET_NOT_FOUND));
    }

    /**
     * Creates a wallet transaction with explicit status.
     *
     * @param wallet                  the wallet to create transaction for
     * @param amount                  the transaction amount
     * @param transactionType         the type of transaction (CREDIT/DEBIT)
     * @param description             human-readable description
     * @param walletTransactionStatus the initial status of the transaction
     */
    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
                                  String description, WalletTransactionStatusEnum walletTransactionStatus) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .status(walletTransactionStatus)
                .transactionType(transactionType)
                .description(description)
                .transactionReference(transactionReferenceGenerator.generate())
                .build();

        walletTransactionRepository.save(newWalletTransaction);
    }

    /**
     * Creates a wallet transaction with explicit status and returns the saved
     * entity.
     * Use this when you need access to the created transaction (e.g., for returning
     * transactionReference).
     *
     * @param wallet                  the wallet to create transaction for
     * @param amount                  the transaction amount
     * @param transactionType         the type of transaction (CREDIT/DEBIT)
     * @param description             human-readable description
     * @param referenceType           type of the reference (e.g., TOPUP, TRANSFER)
     * @param walletTransactionStatus the initial status of the transaction
     * @return the saved wallet transaction entity
     */
    public WalletTransactions createTransactionAndReturn(Wallets wallet, BigDecimal amount,
                                                         TransactionTypeEnum transactionType,
                                                         String description, ReferenceTypeEnum referenceType,
                                                         WalletTransactionStatusEnum walletTransactionStatus) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .status(walletTransactionStatus)
                .transactionType(transactionType)
                .description(description)
                .referenceType(referenceType)
                .transactionReference(transactionReferenceGenerator.generate())
                .createdDate(Instant.now())
                .build();

        return walletTransactionRepository.save(newWalletTransaction);
    }

    /**
     * Creates a wallet transaction with external reference.
     *
     * @param wallet          the wallet to create transaction for
     * @param amount          the transaction amount
     * @param transactionType the type of transaction (CREDIT/DEBIT)
     * @param description     human-readable description
     * @param referenceId     external reference ID (e.g., payment ID)
     * @param referenceType   type of the external reference
     * @param status          the transaction status
     */
    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
                                  String description, String referenceId, ReferenceTypeEnum referenceType, WalletTransactionStatusEnum status) {
        createTransaction(wallet, amount, transactionType, description, referenceId, referenceType, status,
                transactionReferenceGenerator.generate());
    }

    /**
     * Creates a wallet transaction with external reference and returns the saved entity.
     * Use this when you need access to the created transaction (e.g., for returning transactionReference).
     *
     * @param wallet          the wallet to create transaction for
     * @param amount          the transaction amount
     * @param transactionType the type of transaction (CREDIT/DEBIT)
     * @param description     human-readable description
     * @param referenceId     external reference ID (e.g., other wallet ID, payment ID)
     * @param referenceType   type of the external reference
     * @param status          the transaction status
     * @return the saved wallet transaction entity
     */
    public WalletTransactions createTransactionAndReturn(Wallets wallet, BigDecimal amount,
                                                         TransactionTypeEnum transactionType,
                                                         String description, String referenceId,
                                                         ReferenceTypeEnum referenceType,
                                                         WalletTransactionStatusEnum status) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(transactionType)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .transactionReference(transactionReferenceGenerator.generate())
                .status(status)
                .createdDate(Instant.now())
                .build();

        return walletTransactionRepository.save(newWalletTransaction);
    }

    /**
     * Creates a wallet transaction with external reference and explicit transaction reference.
     * Use this when multiple transactions need to share the same reference (e.g., transfers).
     *
     * @param wallet               the wallet to create transaction for
     * @param amount               the transaction amount
     * @param transactionType      the type of transaction (CREDIT/DEBIT)
     * @param description          human-readable description
     * @param referenceId          external reference ID (e.g., payment ID)
     * @param referenceType        type of the external reference
     * @param status               the transaction status
     * @param transactionReference the transaction reference to use
     */
    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
                                  String description, String referenceId, ReferenceTypeEnum referenceType,
                                  WalletTransactionStatusEnum status, String transactionReference) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(transactionType)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .transactionReference(transactionReference)
                .status(status)
                .build();

        walletTransactionRepository.save(newWalletTransaction);
    }

    /**
     * Generates a new transaction reference.
     *
     * @return a unique transaction reference string
     */
    public String generateTransactionReference() {
        return transactionReferenceGenerator.generate();
    }

    /**
     * Calculates the available balance for a wallet.
     * Available balance = total balance - reserved balance
     *
     * @param wallet the wallet to calculate available balance for
     * @return the available balance that can be used for new transactions
     */
    public BigDecimal calculateAvailableBalance(Wallets wallet) {
        return wallet.getBalance().subtract(wallet.getReservedBalance());
    }

    /**
     * Validates that the wallet has sufficient available balance.
     *
     * @param wallet the wallet to check
     * @param amount the required amount
     * @throws BusinessException if available balance is insufficient
     */
    public void validateSufficientBalance(Wallets wallet, BigDecimal amount) {
        BigDecimal availableBalance = calculateAvailableBalance(wallet);
        if (availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, ErrorMessages.INSUFFICIENT_BALANCE);
        }
    }

    /**
     * Validates that source and destination wallets are different.
     *
     * @param wallet1Id first wallet ID
     * @param wallet2Id second wallet ID
     * @throws BusinessException if wallets are the same
     */
    public void validateNotSameWallet(UUID wallet1Id, UUID wallet2Id) {
        if (wallet1Id.equals(wallet2Id)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER, ErrorMessages.CANNOT_TRANSFER_SAME_WALLET);
        }
    }

    /**
     * Parses and validates a user ID string to UUID.
     *
     * @param userId the user ID string to parse
     * @return the parsed UUID
     * @throws BusinessException if user ID is null, blank, or invalid format
     */
    public UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.USER_ID_REQUIRED);
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.INVALID_USER_ID_FORMAT);
        }
    }

    /**
     * Resolves currency with SGD as default.
     *
     * @param currency the currency to resolve (may be null)
     * @return the provided currency or SGD if null
     */
    public CurrencyEnum resolveCurrency(CurrencyEnum currency) {
        return currency != null ? currency : CurrencyEnum.SGD;
    }

    /**
     * Retrieves a wallet transaction by ID.
     *
     * @param walletTransactionId the transaction ID to retrieve
     * @return the wallet transaction entity
     * @throws BusinessException if transaction not found or ID is null
     */
    public WalletTransactions getWalletTransactionById(UUID walletTransactionId) {
        if (walletTransactionId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.INVALID_WALLET_TRANSACTION_ID);
        }

        return walletTransactionRepository.findById(walletTransactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND,
                        ErrorMessages.WALLET_TRANSACTION_NOT_FOUND));
    }

    /**
     * Validates transaction can transition to the target status.
     * Returns true if already in target status (idempotent), false if you can
     * proceed.
     * Throws exception if in invalid state for transition.
     */
    public boolean isTransactionAlreadyInStatus(WalletTransactions transaction,
                                                WalletTransactionStatusEnum targetStatus) {
        WalletTransactionStatusEnum currentStatus = transaction.getStatus();

        if (currentStatus == targetStatus) {
            return true;
        }

        if (currentStatus != WalletTransactionStatusEnum.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    String.format(ErrorMessages.INVALID_TRANSACTION_TRANSITION,
                            targetStatus, currentStatus));
        }

        return false;
    }

    /**
     * Validates that a wallet transaction belongs to the specified wallet and user.
     * Prevents information disclosure by ensuring callers cannot access other
     * users' data.
     *
     * @param transaction the wallet transaction to validate
     * @param walletId    the expected wallet ID
     * @param userId      the authenticated user's ID
     * @throws BusinessException if wallet ID mismatch or user doesn't own the
     *                           wallet
     */
    public void validateTransactionBelongsToWallet(WalletTransactions transaction, UUID walletId, String userId) {
        Wallets wallet = transaction.getWallet();

        if (!wallet.getId().equals(walletId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.WALLET_TRANSACTION_MISMATCH);
        }

        UUID userUuid = parseUserId(userId);
        if (!wallet.getUserId().equals(userUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, ErrorMessages.WALLET_ACCESS_DENIED);
        }
    }
}
