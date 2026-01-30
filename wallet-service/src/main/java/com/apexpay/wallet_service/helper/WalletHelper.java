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
import java.util.UUID;

@Component
public class WalletHelper {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletHelper(WalletRepository walletRepository,
                        WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public Wallets getWalletByUserIdAndId(String userId, UUID walletId) {
        UUID userUuid = parseUserId(userId);

        return walletRepository.findWalletByUserIdAndId(userUuid, walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND, ErrorMessages.WALLET_NOT_FOUND));
    }

    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
                                  String description, WalletTransactionStatusEnum walletTransactionStatus) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .status(walletTransactionStatus)
                .transactionType(transactionType)
                .description(description)
                .build();

        walletTransactionRepository.save(newWalletTransaction);
    }

    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
                                  String description, String referenceId, ReferenceTypeEnum referenceType) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(transactionType)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        walletTransactionRepository.save(newWalletTransaction);
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

    public void validateSufficientBalance(Wallets wallet, BigDecimal amount) {
        BigDecimal availableBalance = calculateAvailableBalance(wallet);
        if (availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, ErrorMessages.INSUFFICIENT_BALANCE);
        }
    }

    public void validateNotSameWallet(UUID wallet1Id, UUID wallet2Id) {
        if (wallet1Id.equals(wallet2Id)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER, ErrorMessages.CANNOT_TRANSFER_SAME_WALLET);
        }
    }

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

    public CurrencyEnum resolveCurrency(CurrencyEnum currency) {
        return currency != null ? currency : CurrencyEnum.SGD;
    }

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
     * Returns true if already in target status (idempotent), false if you can proceed.
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
     * Prevents information disclosure by ensuring callers cannot access other users' data.
     *
     * @param transaction the wallet transaction to validate
     * @param walletId    the expected wallet ID
     * @param userId      the authenticated user's ID
     * @throws BusinessException if wallet ID mismatch or user doesn't own the wallet
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
