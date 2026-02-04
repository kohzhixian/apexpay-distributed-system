package com.apexpay.payment_service.service;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.constants.ResponseMessages;
import com.apexpay.common.constants.TransactionDescriptions;
import com.apexpay.common.dto.*;
import com.apexpay.common.enums.PaymentStatusEnum;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeRequest;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeResponse;
import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.client.provider.enums.ProviderTransactionStatus;
import com.apexpay.payment_service.client.provider.exception.PaymentProviderException;
import com.apexpay.payment_service.client.provider.interfaces.PaymentProviderClient;
import com.apexpay.payment_service.dto.InitiatePaymentRequest;
import com.apexpay.payment_service.dto.InitiatePaymentResponse;
import com.apexpay.payment_service.entity.PaymentMethod;
import com.apexpay.payment_service.entity.Payments;
import com.apexpay.payment_service.repository.PaymentRepository;
import com.apexpay.payment_service.repository.WalletClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for orchestrating payment processing.
 *
 * <p>
 * Implements a two-phase commit pattern for payment processing:
 * <ol>
 * <li>Reserve funds in user's wallet</li>
 * <li>Charge external payment provider</li>
 * <li>Confirm or cancel the reservation based on charge result</li>
 * </ol>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Idempotent payment initiation using clientRequestId</li>
 * <li>Retry mechanism with exponential backoff for transient failures</li>
 * <li>Safe reservation cancellation on failures</li>
 * <li>State tracking for audit and recovery</li>
 * </ul>
 */
@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000; // 1 second

    private final WalletClient walletClient;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final PaymentMethodService paymentMethodService;

    /**
     * Constructs the PaymentService with required dependencies.
     *
     * @param walletClient          Feign client for wallet service communication
     * @param paymentRepository     repository for payment persistence
     * @param paymentProviderClient client for external payment provider
     * @param paymentMethodService  service for payment method operations
     */
    public PaymentService(WalletClient walletClient, PaymentRepository paymentRepository,
                          PaymentProviderClient paymentProviderClient, PaymentMethodService paymentMethodService) {
        this.walletClient = walletClient;
        this.paymentRepository = paymentRepository;
        this.paymentProviderClient = paymentProviderClient;
        this.paymentMethodService = paymentMethodService;
    }

    /**
     * Initiates a new payment request.
     *
     * <p>
     * This method is idempotent - if a payment with the same clientRequestId
     * already exists for the user (and is not expired), returns the existing payment
     * instead of creating a duplicate. Expired payments are ignored, allowing users
     * to create a new payment with the same clientRequestId.
     *
     * @param request the payment initiation request containing amount, currency,
     *                walletId, etc.
     * @param userId  the authenticated user's ID
     * @return response containing paymentId and version for subsequent processing
     * @throws BusinessException if user ID format is invalid
     */
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {
        UUID userUuid = UUID.fromString(userId);

        try {
            // Check if payment already exists (including expired to handle reuse)
            Optional<Payments> existingPayment = paymentRepository
                    .findByClientRequestIdAndUserId(request.clientRequestId(), userUuid);

            if (existingPayment.isPresent()) {
                Payments existing = existingPayment.get();

                // If payment is expired, reuse it by resetting fields
                if (existing.getStatus() == PaymentStatusEnum.EXPIRED) {
                    logger.info("Reusing expired payment. ClientRequestId: {}, PaymentId: {}",
                            request.clientRequestId(), existing.getId());
                    existing.setStatus(PaymentStatusEnum.INITIATED);
                    existing.setAmount(request.amount());
                    existing.setCurrency(request.currency());
                    existing.setWalletId(request.walletId());
                    existing.setProviderTransactionId(null);
                    existing.setProvider(null);
                    existing.setWalletTransactionId(null);
                    existing.setFailureCode(null);
                    existing.setFailureMessage(null);
                    Payments reusedPayment = paymentRepository.save(existing);
                    return new InitiatePaymentResponse(ResponseMessages.PAYMENT_INITIATED, reusedPayment.getId(),
                            reusedPayment.getVersion(), true);
                }

                // Non-expired payment exists - return it (idempotency)
                logger.info("Duplicate payment request detected. ClientRequestId: {}, PaymentId: {}",
                        request.clientRequestId(), existing.getId());
                return new InitiatePaymentResponse(ResponseMessages.RETURNING_EXISTING_PAYMENT, existing.getId(),
                        existing.getVersion(), false);
            }

            // create new payment record
            Payments newPayment = createInitiatePayment(request, userUuid);

            return new InitiatePaymentResponse(ResponseMessages.PAYMENT_INITIATED, newPayment.getId(),
                    newPayment.getVersion(), true);

        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created payment between check and insert
            logger.info("Concurrent payment creation detected for clientRequestId: {}", request.clientRequestId());

            Payments existing = paymentRepository
                    .findByClientRequestIdAndUserId(request.clientRequestId(), userUuid)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            ErrorMessages.PAYMENT_CREATION_FAILED));

            // Handle race condition with expired payment
            if (existing.getStatus() == PaymentStatusEnum.EXPIRED) {
                existing.setStatus(PaymentStatusEnum.INITIATED);
                existing.setAmount(request.amount());
                existing.setCurrency(request.currency());
                existing.setWalletId(request.walletId());
                existing.setProviderTransactionId(null);
                existing.setProvider(null);
                existing.setWalletTransactionId(null);
                existing.setFailureCode(null);
                existing.setFailureMessage(null);
                Payments reusedPayment = paymentRepository.save(existing);
                return new InitiatePaymentResponse(ResponseMessages.PAYMENT_INITIATED, reusedPayment.getId(),
                        reusedPayment.getVersion(), true);
            }

            return new InitiatePaymentResponse(ResponseMessages.RETURNING_EXISTING_PAYMENT, existing.getId(),
                    existing.getVersion(), false);
        }
    }

    /**
     * Processes an initiated payment through the full payment flow.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Validate payment method exists and belongs to user</li>
     * <li>Acquire pessimistic lock on payment record</li>
     * <li>Validate payment exists and belongs to user</li>
     * <li>Validate payment is in INITIATED status</li>
     * <li>Reserve funds in user's wallet</li>
     * <li>Update payment status to PENDING</li>
     * <li>Charge external payment provider (with retries)</li>
     * <li>On success: confirm reservation, update to SUCCESS, update lastUsedAt</li>
     * <li>On failure: cancel reservation, update to FAILED</li>
     * </ol>
     *
     * <p>
     * Uses pessimistic locking (SELECT FOR UPDATE) to prevent race conditions
     * where concurrent requests could both charge the payment provider, resulting
     * in double-charging the user.
     * </p>
     *
     * @param paymentId       the ID of the initiated payment
     * @param userId          the authenticated user's ID
     * @param paymentMethodId the ID of the saved payment method to charge
     * @return response with payment status
     * @throws BusinessException if payment not found, unauthorized, invalid state,
     *                           or charge fails
     */
    @Transactional
    public PaymentResponse processPayment(UUID paymentId, String userId, UUID paymentMethodId) {
        UUID userUuid = UUID.fromString(userId);

        // Validate payment method exists and belongs to user, get provider token
        PaymentMethod paymentMethod = paymentMethodService.validatePaymentMethod(paymentMethodId, userId);
        String paymentMethodToken = paymentMethod.getProviderToken();

        // find existing payment with pessimistic lock to prevent concurrent double-charging
        Payments paymentRecord = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, ErrorMessages.PAYMENT_NOT_FOUND));

        // check if payment record belongs to user
        if (!paymentRecord.getUserId().equals(userUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, ErrorMessages.PAYMENT_ACCESS_DENIED);
        }

        // check if payment status is initiated
        if (paymentRecord.getStatus() != PaymentStatusEnum.INITIATED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format(ErrorMessages.PAYMENT_INVALID_STATUS, paymentRecord.getStatus()));
        }

        UUID walletTransactionId = null;
        try {
            ReserveFundsResponse reserveResponse = walletClient.reserveFunds(
                    new ReserveFundsRequest(paymentRecord.getAmount(), paymentRecord.getCurrency(), paymentId),
                    userId,
                    paymentRecord.getWalletId());
            walletTransactionId = reserveResponse.walletTransactionId();

            ProviderChargeResponse chargeResponse = chargeWithRetry(paymentRecord, paymentMethodToken);

            if (chargeResponse.isSuccessful()) {
                try {
                    walletClient.confirmReservation(
                            new ConfirmReservationRequest(walletTransactionId, chargeResponse.providerTransactionId(),
                                    chargeResponse.providerName()),
                            userId, paymentRecord.getWalletId());
                    logger.info("Reservation confirmed for payment {}. WalletTransactionId: {}", paymentId,
                            walletTransactionId);
                } catch (Exception e) {
                    /*
                     * CRITICAL: Eventual Consistency Logic
                     * If the external charge succeeded but confirming the reservation fails (e.g.,
                     * network error),
                     * we MUST NOT throw an exception. Throwing an exception would roll back the
                     * payment status
                     * to INITIATED/FAILED, telling the user their payment failed even though they
                     * were already
                     * charged externally.
                     *
                     * Instead, we proceed to update the payment to SUCCESS. This ensures our system
                     * matches
                     * the external reality (the user's bank account). The "stuck" reservation in
                     * the wallet
                     * service is considered a "zombie" state that will be resolved by a background
                     * reconciliation process or manual intervention.
                     */
                    logger.error("Failed to confirm reservation for payment {}. WalletTransactionId: {}",
                            paymentId, walletTransactionId, e);
                }

                paymentRecord = updatePaymentToSuccess(paymentRecord, chargeResponse, walletTransactionId);

                // Update lastUsedAt for the payment method (non-blocking)
                updatePaymentMethodLastUsedSafely(paymentMethodId, userId);

                return new PaymentResponse(paymentRecord.getId(), PaymentStatusEnum.SUCCESS,
                        ResponseMessages.PAYMENT_SUCCEEDED, paymentRecord.getAmount(), paymentRecord.getCurrency(),
                        paymentRecord.getCreatedDate(), paymentRecord.getUpdatedDate());
            }

            // Handle PENDING status - payment submitted but not yet confirmed
            if (chargeResponse.status() == ProviderTransactionStatus.PENDING) {
                // Store external transaction ID and wallet transaction ID for later status
                // polling
                paymentRecord = updatePaymentToPendingWithExternalId(paymentRecord, chargeResponse,
                        walletTransactionId);
                logger.info("Payment {} is pending. ProviderTransactionId: {}, WalletTransactionId: {}",
                        paymentRecord.getId(), chargeResponse.providerTransactionId(), walletTransactionId);
                return new PaymentResponse(paymentRecord.getId(), PaymentStatusEnum.PENDING,
                        ResponseMessages.PAYMENT_PENDING, paymentRecord.getAmount(), paymentRecord.getCurrency(),
                        paymentRecord.getCreatedDate(), paymentRecord.getUpdatedDate());
            }

            // handle failure - charge returned failed response
            // Cancel reservation first (HTTP call commits immediately in wallet-service)
            cancelReservationSafely(walletTransactionId, userId, paymentRecord.getWalletId());
            // Update payment status and return response (no exception to avoid rollback)
            paymentRecord = updatePaymentToFailure(paymentRecord, chargeResponse.failureCode(), chargeResponse.failureMessage());
            return new PaymentResponse(paymentRecord.getId(), PaymentStatusEnum.FAILED, chargeResponse.failureMessage(),
                    paymentRecord.getAmount(), paymentRecord.getCurrency(), paymentRecord.getCreatedDate(),
                    paymentRecord.getUpdatedDate());
        } catch (PaymentProviderException e) {
            // Handle exception - cancel reservation and update status
            if (walletTransactionId != null) {
                cancelReservationSafely(walletTransactionId, userId, paymentRecord.getWalletId());
            }
            // Update payment status and return response (no exception to avoid rollback)
            paymentRecord = updatePaymentToFailure(paymentRecord, e.getFailureCode(), e.getMessage());
            return new PaymentResponse(paymentRecord.getId(), PaymentStatusEnum.FAILED, e.getMessage(),
                    paymentRecord.getAmount(), paymentRecord.getCurrency(), paymentRecord.getCreatedDate(),
                    paymentRecord.getUpdatedDate());
        }
    }

    /**
     * Safely updates the lastUsedAt timestamp for a payment method without throwing exceptions.
     * This is a non-critical operation that shouldn't affect the payment success response.
     *
     * @param paymentMethodId the payment method ID to update
     * @param userId          the user ID (for ownership verification)
     */
    private void updatePaymentMethodLastUsedSafely(UUID paymentMethodId, String userId) {
        try {
            paymentMethodService.markAsUsed(paymentMethodId, userId);
        } catch (Exception e) {
            logger.warn("Failed to update lastUsedAt for payment method {}: {}", paymentMethodId, e.getMessage());
        }
    }

    /**
     * Checks the status of a pending payment with the payment provider.
     * <p>
     * Used to poll for status updates when a payment is in PENDING state.
     * Calls the provider's getTransactionStatus() method to retrieve the
     * current transaction status and updates the payment accordingly.
     * </p>
     *
     * @param paymentId the ID of the payment to check
     * @param userId    the authenticated user's ID
     * @return response with updated payment status
     * @throws BusinessException if payment not found, unauthorized, or not in
     *                           PENDING status
     */
    @Transactional
    public PaymentResponse checkPaymentStatus(UUID paymentId, String userId) {
        UUID userUuid = UUID.fromString(userId);
        // Use pessimistic lock to prevent concurrent status checks from triggering
        // duplicate wallet operations (confirmReservation/cancelReservation)
        Payments payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        ErrorMessages.PAYMENT_NOT_FOUND));

        // Check if payment belongs to user
        if (!payment.getUserId().equals(userUuid)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, ErrorMessages.PAYMENT_ACCESS_DENIED);
        }

        // Only check status for PENDING payments
        if (payment.getStatus() != PaymentStatusEnum.PENDING) {
            return new PaymentResponse(payment.getId(), payment.getStatus(),
                    String.format("Payment is already in %s status.", payment.getStatus()),
                    payment.getAmount(), payment.getCurrency(), payment.getCreatedDate(), payment.getUpdatedDate());
        }

        // Must have provider transaction ID to query provider
        if (payment.getProviderTransactionId() == null || payment.getProviderTransactionId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "Payment does not have a provider transaction ID for status checking.");
        }

        try {
            // Query provider for current transaction status
            ProviderChargeResponse providerResponse = paymentProviderClient
                    .getTransactionStatus(payment.getProviderTransactionId());

            if (providerResponse.isSuccessful()) {
                // Payment succeeded - confirm reservation and update status
                UUID walletTxId = payment.getWalletTransactionId();
                if (walletTxId != null) {
                    try {
                        walletClient.confirmReservation(
                                new ConfirmReservationRequest(walletTxId, providerResponse.providerTransactionId(),
                                        providerResponse.providerName()),
                                userId, payment.getWalletId());
                        logger.info("Reservation confirmed for payment {}. WalletTransactionId: {}", paymentId,
                                walletTxId);
                    } catch (Exception e) {
                        logger.error("Failed to confirm reservation for payment {}. WalletTransactionId: {}",
                                paymentId, walletTxId, e);
                        // Continue with payment status update even if confirmation fails
                        // The reservation can be confirmed later via reconciliation
                    }
                } else {
                    logger.warn(
                            "Payment {} succeeded but walletTransactionId is null. Reservation confirmation skipped.",
                            paymentId);
                }

                payment = updatePaymentToSuccess(payment, providerResponse, walletTxId);
                return new PaymentResponse(payment.getId(), PaymentStatusEnum.SUCCESS,
                        ResponseMessages.PAYMENT_SUCCEEDED, payment.getAmount(), payment.getCurrency(),
                        payment.getCreatedDate(), payment.getUpdatedDate());
            } else if (providerResponse.status() == ProviderTransactionStatus.PENDING) {
                // Still pending
                return new PaymentResponse(payment.getId(), PaymentStatusEnum.PENDING,
                        ResponseMessages.PAYMENT_PENDING, payment.getAmount(), payment.getCurrency(),
                        payment.getCreatedDate(), payment.getUpdatedDate());
            } else {
                // Payment failed - cancel reservation and update status
                UUID walletTxId = payment.getWalletTransactionId();
                if (walletTxId != null) {
                    cancelReservationSafely(walletTxId, userId, payment.getWalletId());
                } else {
                    logger.warn("Payment {} failed but walletTransactionId is null. Reservation cancellation skipped.",
                            paymentId);
                }
                // Update payment status and return response (no exception to avoid rollback)
                payment = updatePaymentToFailure(payment, providerResponse.failureCode(),
                        providerResponse.failureMessage());
                return new PaymentResponse(payment.getId(), PaymentStatusEnum.FAILED,
                        providerResponse.failureMessage(), payment.getAmount(), payment.getCurrency(),
                        payment.getCreatedDate(), payment.getUpdatedDate());
            }
        } catch (PaymentProviderException e) {
            logger.error("Error checking payment status with provider. PaymentId: {}", paymentId, e);
            throw new BusinessException(ErrorCode.PAYMENT_CHARGE_FAILED, e.getMessage());
        }
    }

    /**
     * Safely cancels a wallet reservation without throwing exceptions.
     *
     * <p>
     * Used to release reserved funds when payment fails. Errors are logged
     * but not propagated to ensure the main failure handling completes.
     * This is a critical part of maintaining eventual consistency; even if the
     * cancellation fails, the payment status is updated to FAILED so that
     * a background reconciliation process can eventually clean up the "zombie"
     * reservation.
     * </p>
     *
     * @param walletTransactionId the reservation/transaction ID to cancel
     * @param userId              the user's ID for authorization
     * @param walletId            the wallet containing the reservation
     */
    private void cancelReservationSafely(UUID walletTransactionId, String userId, UUID walletId) {
        try {
            walletClient.cancelReservation(
                    new CancelReservationRequest(walletTransactionId),
                    userId,
                    walletId);
        } catch (Exception e) {
            logger.error("Failed to cancel reservation {}:{}", walletTransactionId, e.getMessage());
        }
    }

    /**
     * Charges the payment provider with automatic retry for transient failures.
     *
     * <p>
     * Implements exponential backoff retry strategy:
     * <ul>
     * <li>Attempt 1: immediate</li>
     * <li>Attempt 2: wait 1 second</li>
     * <li>Attempt 3: wait 2 seconds</li>
     * </ul>
     *
     * <p>
     * Retries occur when:
     * <ul>
     * <li>Provider returns a failed response with isRetryable=true</li>
     * <li>PaymentProviderException is thrown with isRetryable=true</li>
     * <li>Unexpected exceptions occur (network issues, timeouts)</li>
     * </ul>
     *
     * @param payment            the payment entity containing amount, currency,
     *                           etc.
     * @param paymentMethodToken token for the payment method
     * @return the charge response from the provider
     * @throws PaymentProviderException if all retries exhausted or non-retryable
     *                                  error
     */
    private ProviderChargeResponse chargeWithRetry(Payments payment, String paymentMethodToken) {
        ProviderChargeResponse lastResponse = null;
        PaymentProviderException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ProviderChargeRequest request = ProviderChargeRequest.of(
                        payment.getId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        paymentMethodToken,
                        TransactionDescriptions.PAYMENT_FOR_ORDER);

                ProviderChargeResponse response = paymentProviderClient.charge(request);
                lastResponse = response;

                // If failed and retryable, and we have retries left
                if (response.isFailed() && response.isRetryable() && attempt < MAX_RETRIES) {
                    logger.warn("Charge failed with retryable error, attempt {}/{}: code={}",
                            attempt, MAX_RETRIES, response.failureCode());
                    waitBeforeRetry(attempt);
                    continue;
                }

                // Return response (success, non-retryable failure, or last attempt failure)
                return response;
            } catch (PaymentProviderException e) {
                lastException = e;
                lastResponse = null; // Clear stale response - exception is more recent failure state
                if (e.isRetryable() && attempt < MAX_RETRIES) {
                    logger.warn("Provider exception (retryable), attempt {}/{}: {}",
                            attempt, MAX_RETRIES, e.getMessage());
                    waitBeforeRetry(attempt);
                    continue;
                }
                throw e;
            } catch (Exception e) {
                logger.error("Unexpected exception during charge, attempt {}/{}", attempt, MAX_RETRIES, e);
                lastException = new PaymentProviderException(
                        String.format(ErrorMessages.UNEXPECTED_ERROR, e.getMessage()),
                        null, true, paymentProviderClient.getProviderName());
                lastResponse = null; // Clear stale response - exception is more recent failure state
                if (attempt < MAX_RETRIES) {
                    waitBeforeRetry(attempt);
                }
            }
        }

        // All retries exhausted - return last response or throw last exception
        if (lastResponse != null) {
            return lastResponse;
        }
        String errorMessage = lastException != null
                ? String.format(ErrorMessages.MAX_RETRIES_EXCEEDED_WITH_MSG, lastException.getMessage())
                : ErrorMessages.MAX_RETRIES_EXCEEDED;
        ProviderFailureCode failureCode = lastException != null ? lastException.getFailureCode() : null;
        throw new PaymentProviderException(errorMessage, failureCode, false, paymentProviderClient.getProviderName());
    }

    /**
     * Waits before the next retry attempt using exponential backoff.
     * Formula: 2^(attempt-1) * BASE_DELAY_MS (1s, 2s, 4s...)
     *
     * @param attempt the current attempt number (1-based)
     * @throws PaymentProviderException if thread is interrupted
     */
    private void waitBeforeRetry(int attempt) {
        try {
            long waitMs = (long) Math.pow(2, attempt - 1) * BASE_DELAY_MS;
            logger.info("Waiting {}ms before retry attempt {}", waitMs, attempt + 1);
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentProviderException(ErrorMessages.RETRY_INTERRUPTED, null, false, "");
        }
    }

    /**
     * Creates and persists a new payment record in INITIATED status.
     *
     * @param request  the payment initiation request
     * @param userUuid the user's UUID
     * @return the saved payment entity
     */
    private Payments createInitiatePayment(InitiatePaymentRequest request, UUID userUuid) {
        Payments newPayment = Payments.builder()
                .amount(request.amount())
                .walletId(request.walletId())
                .clientRequestId(request.clientRequestId())
                .status(PaymentStatusEnum.INITIATED)
                .currency(request.currency())
                .userId(userUuid)
                .build();

        return paymentRepository.save(newPayment);
    }

    /**
     * Updates payment to PENDING status with provider transaction ID and wallet
     * transaction ID.
     * Uses optimistic locking to prevent concurrent modification conflicts.
     * <p>
     * Used after provider responds (SUCCESS or PENDING) to store the provider
     * transaction ID
     * and wallet transaction ID. This ensures we have the necessary information
     * for:
     * <ul>
     * <li>Status polling (if PENDING)</li>
     * <li>Reconciliation (if SUCCESS)</li>
     * <li>Audit trail</li>
     * </ul>
     * </p>
     *
     * @param payment             the payment entity to update
     * @param response            the charge response from provider (SUCCESS or
     *                            PENDING)
     * @param walletTransactionId the wallet transaction ID created during fund
     *                            reservation
     * @return the updated and persisted payment entity
     * @throws BusinessException if concurrent modification detected (version
     *                           mismatch)
     */
    private Payments updatePaymentToPendingWithExternalId(Payments payment, ProviderChargeResponse response,
                                                          UUID walletTransactionId) {
        Long currentVersion = payment.getVersion();
        int updated = paymentRepository.updatePaymentPending(
                payment.getId(),
                response.providerTransactionId(),
                response.providerName(),
                walletTransactionId,
                currentVersion);

        if (updated == 0) {
            logger.warn("Concurrent payment update detected. PaymentId: {}, ExpectedVersion: {}",
                    payment.getId(), currentVersion);
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                    ErrorMessages.CONCURRENT_PAYMENT_UPDATE);
        }

        // Reload to get updated entity with new version
        return paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        ErrorMessages.PAYMENT_NOT_FOUND));
    }

    /**
     * Updates payment to SUCCESS status with provider transaction details.
     * Uses optimistic locking to prevent concurrent modification conflicts.
     * Stores providerTransactionId, providerName, and walletTransactionId for reconciliation.
     * <p>
     * The walletTransactionId is critical for reconciliation - if confirmReservation fails
     * after a successful charge, this ID enables identifying and resolving "zombie" reservations.
     * </p>
     *
     * @param payment             the payment entity to update
     * @param response            the successful charge response from provider
     * @param walletTransactionId the wallet transaction ID for reconciliation (may be null for checkPaymentStatus path)
     * @return the updated and persisted payment entity
     * @throws BusinessException if concurrent modification detected (version
     *                           mismatch)
     */
    private Payments updatePaymentToSuccess(Payments payment, ProviderChargeResponse response, UUID walletTransactionId) {
        Long currentVersion = payment.getVersion();
        int updated = paymentRepository.updatePaymentSuccess(
                payment.getId(),
                response.providerTransactionId(),
                response.providerName(),
                walletTransactionId,
                currentVersion);

        if (updated == 0) {
            logger.warn("Concurrent payment update detected. PaymentId: {}, ExpectedVersion: {}",
                    payment.getId(), currentVersion);
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                    ErrorMessages.CONCURRENT_PAYMENT_UPDATE);
        }

        // Reload to get updated entity with new version
        return paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        ErrorMessages.PAYMENT_NOT_FOUND));
    }

    /**
     * Updates payment to FAILED status with failure details.
     * Uses optimistic locking to prevent concurrent modification conflicts.
     * Stores failureCode and failureMessage for debugging and customer support.
     *
     * @param payment        the payment entity to update
     * @param failureCode    the provider-specific failure code (maybe null)
     * @param failureMessage human-readable failure description
     * @return the updated and persisted payment entity
     * @throws BusinessException if concurrent modification detected (version
     *                           mismatch)
     */
    private Payments updatePaymentToFailure(Payments payment, ProviderFailureCode failureCode,
                                            String failureMessage) {
        Long currentVersion = payment.getVersion();
        int updated = paymentRepository.updatePaymentFailed(
                payment.getId(),
                failureCode,
                failureMessage,
                currentVersion);

        if (updated == 0) {
            logger.warn("Concurrent payment update detected. PaymentId: {}, ExpectedVersion: {}",
                    payment.getId(), currentVersion);
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                    ErrorMessages.CONCURRENT_PAYMENT_UPDATE);
        }

        // Reload to get updated entity with new version
        return paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        ErrorMessages.PAYMENT_NOT_FOUND));
    }
}
