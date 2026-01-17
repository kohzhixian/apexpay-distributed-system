package com.apexpay.wallet_service.service;

import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.wallet_service.dto.request.CreateWalletRequest;
import com.apexpay.wallet_service.dto.request.PaymentRequest;
import com.apexpay.wallet_service.dto.request.TopUpWalletRequest;
import com.apexpay.wallet_service.dto.request.TransferRequest;
import com.apexpay.wallet_service.dto.response.*;
import com.apexpay.wallet_service.entity.Wallets;
import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.helper.WalletHelper;
import com.apexpay.wallet_service.repository.WalletRepository;
import com.apexpay.wallet_service.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service handling wallet operations including creation, top-up, transfers, and
 * payments.
 * Uses optimistic locking with retry mechanism to handle concurrent updates.
 */
@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletHelper walletHelper;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletHelper walletHelper) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletHelper = walletHelper;
    }

    /**
     * Creates a new wallet for the specified user with initial balance.
     */
    @Transactional
    public CreateWalletResponse createWallet(CreateWalletRequest request, String userId) {
        UUID userUuid = walletHelper.parseUserId(userId);
        BigDecimal defaultReservedBalance = new BigDecimal("0.00");
        Wallets newWallet = Wallets.builder()
                .balance(request.balance())
                .userId(userUuid)
                .reservedBalance(defaultReservedBalance)
                .currency(walletHelper.resolveCurrency(request.currency()))
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
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TopUpWalletResponse topUpWallet(TopUpWalletRequest request, String userId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, request.walletId());

        // Calculate new balance of wallet
        BigDecimal newBalance = existingWallet.getBalance().add(request.amount());
        existingWallet.setBalance(newBalance);
        walletRepository.save(existingWallet);

        walletHelper.createTransaction(existingWallet, request.amount(), TransactionTypeEnum.CREDIT, "Top up wallet");
        logger.info("Top up successful. WalletId: {}, Amount: {}", existingWallet.getId(), request.amount());
        return new TopUpWalletResponse("Wallet top up successfully.");
    }

    /**
     * Recovery handler when top-up retries are exhausted.
     */
    @Recover
    public TopUpWalletResponse recoverTopUp(ObjectOptimisticLockingFailureException ex, TopUpWalletRequest request,
            String userId) {
        logger.error("Topup failed after retries. UserId: {} , WalletID: {}", userId, request.walletId());
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    /**
     * Transfers funds from payer wallet to receiver wallet.
     * Retries up to 3 times on concurrent modification conflicts.
     */
    @Transactional
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TransferResponse transfer(TransferRequest request, String payerUserId) {
        walletHelper.validateNotSameWallet(request.payerWalletId(), request.receiverWalletId());

        // get payer wallet
        Wallets payerWallet = walletHelper.getWalletByUserIdAndId(payerUserId, request.payerWalletId());

        // get receiver wallet
        Wallets receiverWallet = walletHelper.getWalletByUserIdAndId(request.receiverUserId(),
                request.receiverWalletId());

        // ! TODO: Check if currency matches for both wallets

        // check if payer wallet has enough balance for transfer
        walletHelper.validateSufficientBalance(payerWallet, request.amount());

        // deduct amount from payer wallet
        BigDecimal payerBalance = payerWallet.getBalance().subtract(request.amount());
        payerWallet.setBalance(payerBalance);
        walletRepository.save(payerWallet);

        // add a new wallet transaction for payer wallet
        walletHelper.createTransaction(payerWallet, request.amount(), TransactionTypeEnum.DEBIT, "Transfer money.",
                request.receiverWalletId(), ReferenceTypeEnum.TRANSFER);

        // Add amount to receiver wallet
        BigDecimal receiverBalance = receiverWallet.getBalance().add(request.amount());
        receiverWallet.setBalance(receiverBalance);
        walletRepository.save(receiverWallet);

        // add a new wallet transaction for receiver wallet
        walletHelper.createTransaction(receiverWallet, request.amount(), TransactionTypeEnum.CREDIT,
                "Received money from transfer.",
                request.payerWalletId(), ReferenceTypeEnum.TRANSFER);

        logger.info("Transfer successful. From: {}, To: {}, Amount: {}",
                payerWallet.getId(), receiverWallet.getId(), request.amount());
        return new TransferResponse("Transfer made successfully.");
    }

    /**
     * Recovery handler when transfer retries are exhausted.
     */
    @Recover
    public TransferResponse recoverTransfer(ObjectOptimisticLockingFailureException e, TransferRequest request,
            String payerUserId) {
        logger.error("Transfer failed after retries. PayerUserId: {}", payerUserId);
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    /**
     * Processes a payment from the user's wallet.
     * Retries up to 3 times on concurrent modification conflicts.
     */
    @Transactional
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public PaymentResponse payment(PaymentRequest request, String userId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, request.walletId());

        // check if user wallet has enough balance to make payment
        walletHelper.validateSufficientBalance(existingWallet, request.amount());

        // calculate wallet new balance
        BigDecimal newBalance = existingWallet.getBalance().subtract(request.amount());
        existingWallet.setBalance(newBalance);
        walletRepository.save(existingWallet);

        // Create new wallet transaction
        walletHelper.createTransaction(existingWallet, request.amount(), TransactionTypeEnum.DEBIT, "Make payment",
                request.referenceId(), ReferenceTypeEnum.PAYMENT);

        logger.info("Payment successful. WalletId: {}, Amount: {}", existingWallet.getId(), request.amount());
        return new PaymentResponse("Payment made successfully.");
    }

    /**
     * Recovery handler when payment retries are exhausted.
     */
    @Recover
    public PaymentResponse recoverPayment(ObjectOptimisticLockingFailureException e, PaymentRequest request,
            String userId) {
        logger.error("Payment failed after retries. UserId: {}, WalletId: {}", userId, request.walletId());
        throw new BusinessException(ErrorCode.CONCURRENT_UPDATE, "Too many concurrent updates. Please try again.");
    }

    /**
     * Returns the wallet balance for the specified wallet owned by the user.
     */
    public GetBalanceResponse getBalance(String walletId, String userId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);
        logger.info("Fetching balance for wallet id: {} and user id: {}", walletId, userId);
        return new GetBalanceResponse(existingWallet.getBalance());
    }

    /**
     * Returns transaction history for the wallet, sorted by latest first.
     * Offset is 1-based; each page returns 10 items.
     */
    public List<GetTransactionHistoryResponse> getTransactionHistory(String walletId, String userId, int offset) {
        UUID walletUuid = walletHelper.parseWalletId(walletId);
        // check if wallet belongs to user
        walletHelper.getWalletByUserIdAndId(userId, walletId);

        int pageIndex = Math.max(offset - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
        return walletTransactionRepository.findAllByWalletId(walletUuid, pageable)
                .stream()
                .map(wt -> new GetTransactionHistoryResponse(
                        wt.getId(), wt.getAmount(), wt.getTransactionType(), wt.getReferenceId(),
                        wt.getReferenceType()))
                .toList();
    }
}
