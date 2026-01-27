package com.apexpay.wallet_service.service;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.constants.ResponseMessages;
import com.apexpay.common.constants.TransactionDescriptions;
import com.apexpay.common.dto.CancelReservationRequest;
import com.apexpay.common.dto.ConfirmReservationRequest;
import com.apexpay.common.dto.ReserveFundsRequest;
import com.apexpay.common.dto.ReserveFundsResponse;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.wallet_service.dto.request.CreateWalletRequest;
import com.apexpay.wallet_service.dto.request.TopUpWalletRequest;
import com.apexpay.wallet_service.dto.request.TransferRequest;
import com.apexpay.wallet_service.dto.response.*;
import com.apexpay.wallet_service.entity.WalletTransactions;
import com.apexpay.wallet_service.entity.Wallets;
import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;
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
import java.util.Optional;
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
     * <p>
     * Initializes a wallet with the provided balance and currency. The reserved
     * balance is set to zero. The wallet is associated with the authenticated user.
     * </p>
     *
     * @param request the wallet creation request containing balance and currency
     * @param userId  the authenticated user's ID
     * @return response confirming wallet creation
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
        return new CreateWalletResponse(ResponseMessages.WALLET_CREATED);
    }

    /**
     * Adds funds to an existing wallet.
     * <p>
     * Increases the wallet's available balance by the specified amount and creates
     * a CREDIT transaction record. Uses optimistic locking with automatic retry
     * (up to 3 times) to handle concurrent modification conflicts.
     * </p>
     *
     * @param request the top-up request containing amount and wallet ID
     * @param userId  the authenticated user's ID
     * @return response confirming successful top-up
     */
    @Transactional
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, backoff = @Backoff(delay = 100))
    public TopUpWalletResponse topUpWallet(TopUpWalletRequest request, String userId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, request.walletId());

        // Calculate new balance of wallet
        BigDecimal newBalance = existingWallet.getBalance().add(request.amount());
        existingWallet.setBalance(newBalance);
        walletRepository.save(existingWallet);

        walletHelper.createTransaction(existingWallet, request.amount(), TransactionTypeEnum.CREDIT,
                TransactionDescriptions.TOP_UP_WALLET, WalletTransactionStatusEnum.COMPLETED);
        logger.info("Top up successful. WalletId: {}, Amount: {}", existingWallet.getId(), request.amount());
        return new TopUpWalletResponse(ResponseMessages.WALLET_TOPUP_SUCCESS);
    }

    /**
     * Recovery handler when top-up retries are exhausted.
     */
    @Recover
    public TopUpWalletResponse recoverTopUp(ObjectOptimisticLockingFailureException ex, TopUpWalletRequest request,
            String userId) {
        logger.error("Topup failed after retries. UserId: {} , WalletID: {}", userId, request.walletId());
        throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, ErrorMessages.CONCURRENT_UPDATE_RETRY);
    }

    /**
     * Transfers funds from payer wallet to receiver wallet.
     * <p>
     * Atomically deducts funds from the payer's wallet and adds them to the
     * receiver's
     * wallet. Creates DEBIT and CREDIT transaction records respectively. Validates
     * that
     * wallets are different and payer has sufficient balance. Uses optimistic
     * locking
     * with automatic retry (up to 3 times) to handle concurrent modification
     * conflicts.
     * </p>
     *
     * @param request     the transfer request containing payer/receiver wallet IDs
     *                    and amount
     * @param payerUserId the authenticated user ID of the payer
     * @return response confirming successful transfer
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
        walletHelper.createTransaction(payerWallet, request.amount(), TransactionTypeEnum.DEBIT,
                TransactionDescriptions.TRANSFER_DEBIT,
                request.receiverWalletId().toString(), ReferenceTypeEnum.TRANSFER);

        // Add amount to receiver wallet
        BigDecimal receiverBalance = receiverWallet.getBalance().add(request.amount());
        receiverWallet.setBalance(receiverBalance);
        walletRepository.save(receiverWallet);

        // add a new wallet transaction for receiver wallet
        walletHelper.createTransaction(receiverWallet, request.amount(), TransactionTypeEnum.CREDIT,
                TransactionDescriptions.TRANSFER_CREDIT,
                request.payerWalletId().toString(), ReferenceTypeEnum.TRANSFER);

        logger.info("Transfer successful. From: {}, To: {}, Amount: {}",
                payerWallet.getId(), receiverWallet.getId(), request.amount());
        return new TransferResponse(ResponseMessages.TRANSFER_SUCCESS);
    }

    /**
     * Recovery handler when transfer retries are exhausted.
     */
    @Recover
    public TransferResponse recoverTransfer(ObjectOptimisticLockingFailureException e, TransferRequest request,
            String payerUserId) {
        logger.error("Transfer failed after retries. PayerUserId: {}", payerUserId);
        throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, ErrorMessages.CONCURRENT_UPDATE_RETRY);
    }

    /**
     * Returns the wallet balance for the specified wallet owned by the user.
     * <p>
     * Retrieves the current available balance of the wallet. The reserved balance
     * is not included in this response.
     * </p>
     *
     * @param walletId the wallet ID to query
     * @param userId   the authenticated user's ID
     * @return response containing the wallet balance
     */
    public GetBalanceResponse getBalance(UUID walletId, String userId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);
        logger.info("Fetching balance for wallet id: {} and user id: {}", walletId, userId);
        return new GetBalanceResponse(existingWallet.getBalance());
    }

    /**
     * Returns transaction history for the wallet, sorted by latest first.
     * <p>
     * Retrieves a paginated list of wallet transactions. The offset is 1-based
     * (offset 1 = first page) and each page contains 10 items. Transactions are
     * sorted by creation date in descending order (newest first).
     * </p>
     *
     * @param walletId the wallet ID to query
     * @param userId   the authenticated user's ID
     * @param offset   the page offset (1-based, where 1 = first page)
     * @return list of transaction history responses (max 10 items)
     */
    public List<GetTransactionHistoryResponse> getTransactionHistory(UUID walletId, String userId, int offset) {
        // check if wallet belongs to user
        walletHelper.getWalletByUserIdAndId(userId, walletId);

        int pageIndex = Math.max(offset - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
        return walletTransactionRepository.findAllByWalletId(walletId, pageable)
                .stream()
                .map(wt -> new GetTransactionHistoryResponse(
                        wt.getId(), wt.getAmount(), wt.getTransactionType(), wt.getReferenceId(),
                        wt.getReferenceType()))
                .toList();
    }

    /**
     * Reserves funds in a wallet for a pending payment.
     * <p>
     * Moves the specified amount from available balance to reserved balance,
     * creating a PENDING wallet transaction. This is the first phase of the
     * two-phase commit pattern for payment processing. Uses optimistic locking
     * to ensure atomicity.
     * </p>
     *
     * @param request  the reservation request containing amount, currency, and
     *                 paymentId
     * @param userId   the authenticated user's ID
     * @param walletId the wallet ID to reserve funds from
     * @return response containing walletTransactionId and remaining balance
     */
    @Transactional
    public ReserveFundsResponse reserveFunds(ReserveFundsRequest request, String userId, UUID walletId) {
        // Idempotency check: check if reservation already exists for this paymentId
        Optional<WalletTransactions> existingTx = walletTransactionRepository
                .findByReferenceIdAndReferenceType(request.paymentId().toString(), ReferenceTypeEnum.PAYMENT);

        if (existingTx.isPresent()) {
            WalletTransactions tx = existingTx.get();
            logger.info("Duplicate reservation request detected. Returning existing transaction: {}", tx.getId());
            Wallets wallet = tx.getWallet();
            // Calculate remaining balance based on current wallet state
            BigDecimal remainingBalance = wallet.getBalance().subtract(wallet.getReservedBalance());
            return new ReserveFundsResponse(tx.getId(), wallet.getId(), tx.getAmount(), remainingBalance);
        }

        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);

        walletHelper.validateSufficientBalance(existingWallet, request.amount());

        // update reserve funds
        int updatedSuccess = updateReserveFunds(request.amount(), walletId, userId, existingWallet.getVersion());

        if (updatedSuccess == 0) {
            handleReservationFailure(userId, walletId, request.amount(), existingWallet.getVersion());
        }

        // create pending transaction
        WalletTransactions newWalletTransaction = createPendingTransaction(existingWallet, request.amount(),
                request.paymentId());

        logger.info("Funds reserved. WalletId: {}, Amount: {}, TransactionId: {}",
                existingWallet.getId(), request.amount(), newWalletTransaction.getId());

        BigDecimal remainingBalance = existingWallet.getBalance().subtract(request.amount());

        return new ReserveFundsResponse(newWalletTransaction.getId(), walletId, request.amount(), remainingBalance);
    }

    /**
     * Confirms a fund reservation after successful payment provider charge.
     * <p>
     * Moves reserved funds to completed state, deducts from balance, and updates
     * the wallet transaction to COMPLETED status. This is the commit phase of the
     * two-phase commit pattern. The operation is idempotent - if already completed,
     * returns success without error.
     * </p>
     *
     * @param request  the confirmation request containing walletTransactionId,
     *                 externalTransactionId, and provider name
     * @param userId   the authenticated user's ID
     * @param walletId the wallet ID containing the reservation
     * @return success message (or "already completed" if idempotent call)
     */
    @Transactional
    public String confirmReservation(ConfirmReservationRequest request, String userId, UUID walletId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);
        WalletTransactions transaction = walletHelper.getWalletTransactionById(request.walletTransactionId());

        // check if wallet transaction belongs to wallet
        if (!transaction.getWallet().getId().equals(walletId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.WALLET_TRANSACTION_MISMATCH);
        }

        // idempotency check
        if (walletHelper.isTransactionAlreadyInStatus(transaction, WalletTransactionStatusEnum.COMPLETED)) {
            logger.info("Transaction already completed. TransactionId: {}", transaction.getId());
            return ResponseMessages.RESERVATION_ALREADY_COMPLETED;
        }

        // deduct from balance (use transaction amount)
        BigDecimal remainingBalance = existingWallet.getBalance().subtract(transaction.getAmount());
        existingWallet.setBalance(remainingBalance);

        // remove from reserved funds
        removeReserveFunds(existingWallet, transaction.getAmount());

        // change status of wallet transaction to completed
        transaction.setStatus(WalletTransactionStatusEnum.COMPLETED);
        walletRepository.save(existingWallet);
        walletTransactionRepository.save(transaction);
        logger.info("Funds reservation completed. WalletId: {}, TransactionId: {}",
                walletId, transaction.getId());

        return ResponseMessages.RESERVATION_COMPLETED;
    }

    /**
     * Cancels a fund reservation when payment fails.
     * <p>
     * Releases reserved funds back to available balance and updates the wallet
     * transaction to CANCELLED status. This is the rollback phase of the two-phase
     * commit pattern. The operation is idempotent - if already cancelled, returns
     * success without error.
     * </p>
     *
     * @param request  the cancellation request containing walletTransactionId
     * @param userId   the authenticated user's ID
     * @param walletId the wallet ID containing the reservation
     * @return success message (or "already cancelled" if idempotent call)
     */
    @Transactional
    public String cancelReservation(CancelReservationRequest request, String userId, UUID walletId) {
        Wallets existingWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);
        WalletTransactions transaction = walletHelper.getWalletTransactionById(request.walletTransactionId());

        // check if wallet transaction belongs to wallet
        if (!transaction.getWallet().getId().equals(walletId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.WALLET_TRANSACTION_MISMATCH);
        }

        // idempotency check
        if (walletHelper.isTransactionAlreadyInStatus(transaction, WalletTransactionStatusEnum.CANCELLED)) {
            logger.info("Transaction already cancelled. TransactionId: {}", transaction.getId());
            return ResponseMessages.RESERVATION_ALREADY_CANCELLED;
        }

        // remove from reserved funds
        removeReserveFunds(existingWallet, transaction.getAmount());

        // change status of wallet transaction to cancelled
        transaction.setStatus(WalletTransactionStatusEnum.CANCELLED);

        walletRepository.save(existingWallet);
        walletTransactionRepository.save(transaction);
        logger.info("Funds reservation cancelled. WalletId: {}, TransactionId: {}",
                walletId, transaction.getId());

        return ResponseMessages.RESERVATION_CANCELLED;
    }

    private int updateReserveFunds(BigDecimal amount, UUID walletId, String userId, Long version) {
        UUID userUuid = walletHelper.parseUserId(userId);

        return walletRepository.tryReserveFunds(amount, walletId, userUuid, version);
    }

    private void handleReservationFailure(String userId, UUID walletId, BigDecimal amount, Long originalVersion) {
        // re fetch wallet data to get current state
        Wallets currentWallet = walletHelper.getWalletByUserIdAndId(userId, walletId);

        // check if version changed (concurrent update)
        if (!currentWallet.getVersion().equals(originalVersion)) {
            logger.warn("Concurrent update detected. WalletId: {}, OriginalVersion: {}, CurrentVersion: {}",
                    walletId, originalVersion, currentWallet.getVersion());
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, ErrorMessages.WALLET_MODIFIED_RETRY);
        }

        // check if current wallet has insufficient funds
        walletHelper.validateSufficientBalance(currentWallet, amount);

        // Unknown reason
        logger.error("Unknown reservation failure. WalletId: {}, Amount: {}", walletId, amount);
        throw new BusinessException(ErrorCode.RESERVATION_FAILURE, ErrorMessages.RESERVATION_FAILURE);
    }

    private WalletTransactions createPendingTransaction(Wallets wallet, BigDecimal amount, UUID paymentId) {
        WalletTransactions newWalletTransaction = WalletTransactions.builder()
                .wallet(wallet)
                .referenceId(paymentId.toString())
                .referenceType(ReferenceTypeEnum.PAYMENT)
                .description(TransactionDescriptions.RESERVE_FUNDS)
                .transactionType(TransactionTypeEnum.DEBIT)
                .amount(amount)
                .status(WalletTransactionStatusEnum.PENDING)
                .build();

        return walletTransactionRepository.save(newWalletTransaction);
    }

    private void removeReserveFunds(Wallets wallet, BigDecimal amount) {
        if (wallet == null || amount == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, ErrorMessages.INVALID_INPUT);
        }

        BigDecimal remainingReserveFunds = wallet.getReservedBalance().subtract(amount);

        // sanity check to prevent negative reserved balance
        if (remainingReserveFunds.compareTo(BigDecimal.ZERO) < 0) {
            logger.error("Reserved balance would become negative. WalletId: {}, Current: {}, Amount: {}",
                    wallet.getId(), wallet.getReservedBalance(), amount);
            throw new BusinessException(ErrorCode.INVALID_STATE, ErrorMessages.INVALID_RESERVED_BALANCE_STATE);
        }

        wallet.setReservedBalance(remainingReserveFunds);
        logger.info("{} removed from reserved funds for wallet id: {}", amount, wallet.getId());
    }
}
