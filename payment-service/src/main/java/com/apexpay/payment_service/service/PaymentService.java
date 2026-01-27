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
import com.apexpay.payment_service.client.provider.exception.PaymentProviderException;
import com.apexpay.payment_service.client.provider.interfaces.PaymentProviderClient;
import com.apexpay.payment_service.dto.InitiatePaymentRequest;
import com.apexpay.payment_service.dto.InitiatePaymentResponse;
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

    public PaymentService(WalletClient walletClient, PaymentRepository paymentRepository,
            PaymentProviderClient paymentProviderClient) {
        this.walletClient = walletClient;
        this.paymentRepository = paymentRepository;
        this.paymentProviderClient = paymentProviderClient;
    }

    /**
     * Initiates a new payment request.
     *
     * <p>
     * This method is idempotent - if a payment with the same clientRequestId
     * already exists for the user, returns the existing payment instead of creating
     * a duplicate.
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
            // Check if payment already exists
            Optional<Payments> existingPayment = paymentRepository
                    .findByClientRequestIdAndUserId(request.clientRequestId(), userUuid);

            if (existingPayment.isPresent()) {
                Payments existing = existingPayment.get();
                logger.info("Duplicate payment request detected. ClientRequestId: {}, PaymentId: {}",
                        request.clientRequestId(), existing.getId());
                return new InitiatePaymentResponse(ResponseMessages.RETURNING_EXISTING_PAYMENT, existing.getId(),
                        existing.getVersion());
            }

            // create new payment record
            Payments newPayment = createInitiatePayment(request, userUuid);

            return new InitiatePaymentResponse(ResponseMessages.PAYMENT_INITIATED, newPayment.getId(),
                    newPayment.getVersion());

        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created payment between check and insert
            logger.info("Concurrent payment creation detected for clientRequestId: {}", request.clientRequestId());

            Payments existing = paymentRepository
                    .findByClientRequestIdAndUserId(request.clientRequestId(), userUuid)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            ErrorMessages.PAYMENT_CREATION_FAILED));

            return new InitiatePaymentResponse(ResponseMessages.RETURNING_EXISTING_PAYMENT, existing.getId(),
                    existing.getVersion());
        }
    }

    /**
     * Processes an initiated payment through the full payment flow.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Validate payment exists and belongs to user</li>
     * <li>Validate payment is in INITIATED status</li>
     * <li>Reserve funds in user's wallet</li>
     * <li>Update payment status to PENDING</li>
     * <li>Charge external payment provider (with retries)</li>
     * <li>On success: confirm reservation, update to SUCCESS</li>
     * <li>On failure: cancel reservation, update to FAILED</li>
     * </ol>
     *
     * @param paymentId          the ID of the initiated payment
     * @param userId             the authenticated user's ID
     * @param paymentMethodToken token representing the payment method (card, etc.)
     * @return response with payment status
     * @throws BusinessException if payment not found, unauthorized, invalid state,
     *                           or charge fails
     */
    public PaymentResponse processPayment(UUID paymentId, String userId, String paymentMethodToken) {
        UUID userUuid = UUID.fromString(userId);
        // find existing payment with payment id
        Payments paymentRecord = paymentRepository.findById(paymentId)
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

            paymentRecord = updatePaymentToPending(paymentRecord);

            ProviderChargeResponse chargeResponse = chargeWithRetry(paymentRecord, paymentMethodToken);

            if (chargeResponse.isSuccessful()) {
                walletClient.confirmReservation(
                        new ConfirmReservationRequest(walletTransactionId, chargeResponse.externalTransactionId(),
                                chargeResponse.providerName()),
                        userId, paymentRecord.getWalletId());

                paymentRecord = updatePaymentToSuccess(paymentRecord, chargeResponse);

                return new PaymentResponse(paymentRecord.getId(), PaymentStatusEnum.SUCCESS,
                        ResponseMessages.PAYMENT_SUCCEEDED);
            }

            // handle failure - charge returned failed response
            cancelReservationSafely(walletTransactionId, userId, paymentRecord.getWalletId());
            updatePaymentToFailure(paymentRecord, chargeResponse.failureCode(), chargeResponse.failureMessage());

            throw new BusinessException(ErrorCode.PAYMENT_CHARGE_FAILED, chargeResponse.failureMessage());
        } catch (PaymentProviderException e) {
            // Handle exception - cancel reservation and update status
            if (walletTransactionId != null) {
                cancelReservationSafely(walletTransactionId, userId, paymentRecord.getWalletId());
            }

            updatePaymentToFailure(paymentRecord, e.getFailureCode(), e.getMessage());

            throw new BusinessException(ErrorCode.PAYMENT_CHARGE_FAILED, e.getMessage());
        }
    }

    /**
     * Safely cancels a wallet reservation without throwing exceptions.
     *
     * <p>
     * Used to release reserved funds when payment fails. Errors are logged
     * but not propagated to ensure the main failure handling completes.
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

    private Payments createInitiatePayment(InitiatePaymentRequest request, UUID userUuid) {
        Payments newPayment = Payments.builder()
                .amount(request.amount())
                .walletId(request.walletId())
                .clientRequestId(request.clientRequestId())
                .status(PaymentStatusEnum.INITIATED)
                .currency(request.currency())
                .provider(request.provider())
                .userId(userUuid)
                .build();

        return paymentRepository.save(newPayment);
    }

    /**
     * Updates payment status to PENDING and persists immediately.
     * Runs in its own transaction to ensure state is saved even if later operations
     * fail.
     *
     * @param payment the payment entity to update
     * @return the updated and persisted payment entity
     */
    @Transactional
    protected Payments updatePaymentToPending(Payments payment) {
        payment.setStatus(PaymentStatusEnum.PENDING);
        return paymentRepository.save(payment);
    }

    /**
     * Updates payment to SUCCESS status with provider transaction details.
     * Stores externalTransactionId for reconciliation and refund operations.
     *
     * @param payment  the payment entity to update
     * @param response the successful charge response from provider
     * @return the updated and persisted payment entity
     */
    @Transactional
    protected Payments updatePaymentToSuccess(Payments payment, ProviderChargeResponse response) {
        payment.setStatus(PaymentStatusEnum.SUCCESS);
        payment.setExternalTransactionId(response.externalTransactionId());
        payment.setProvider(response.providerName());
        return paymentRepository.save(payment);
    }

    /**
     * Updates payment to FAILED status with failure details.
     * Stores failureCode and failureMessage for debugging and customer support.
     *
     * @param payment        the payment entity to update
     * @param failureCode    the provider-specific failure code (maybe null)
     * @param failureMessage human-readable failure description
     * @return the updated and persisted payment entity
     */
    @Transactional
    protected Payments updatePaymentToFailure(Payments payment, ProviderFailureCode failureCode,
            String failureMessage) {
        payment.setStatus(PaymentStatusEnum.FAILED);
        payment.setFailureCode(failureCode);
        payment.setFailureMessage(failureMessage);
        return paymentRepository.save(payment);
    }
}
