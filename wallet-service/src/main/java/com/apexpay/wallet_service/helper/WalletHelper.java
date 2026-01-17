package com.apexpay.wallet_service.helper;

import com.apexpay.common.enums.CurrencyEnum;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.entity.Wallets;
import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
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

    public Wallets getWalletByUserIdAndId(String userId, String walletId) {
        UUID userUuid = parseUserId(userId);
        UUID walletUuid = parseWalletId(walletId);

        return walletRepository.findWalletByUserIdAndId(userUuid, walletUuid)
                .orElseThrow(() -> {
                    return new BusinessException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found.");
                });
    }

    public void createTransaction(Wallets wallet, BigDecimal amount, TransactionTypeEnum transactionType,
            String description) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .amount(amount)
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

    public void validateSufficientBalance(Wallets wallet, BigDecimal amount) {
        BigDecimal balance = wallet.getBalance().subtract(wallet.getReservedBalance());
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient balance.");
        }
    }

    public void validateNotSameWallet(String wallet1Id, String wallet2Id) {
        UUID wallet1Uuid = parseWalletId(wallet1Id);
        UUID wallet2Uuid = parseWalletId(wallet2Id);
        if (wallet1Uuid.equals(wallet2Uuid)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER, "Cannot transfer to the same wallet.");
        }
    }

    public UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "User id is required.");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid User id format.");
        }
    }

    public UUID parseWalletId(String walletId) {
        if (walletId == null || walletId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Wallet id is required.");
        }
        try {
            return UUID.fromString(walletId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid Wallet id format.");
        }
    }

    public CurrencyEnum resolveCurrency(CurrencyEnum currency) {
        return currency != null ? currency : CurrencyEnum.SGD;
    }
}
