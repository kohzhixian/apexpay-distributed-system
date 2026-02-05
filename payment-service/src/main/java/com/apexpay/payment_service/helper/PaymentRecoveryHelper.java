package com.apexpay.payment_service.helper;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.enums.PaymentStatusEnum;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.payment_service.dto.InitiatePaymentRequest;
import com.apexpay.payment_service.entity.Payments;
import com.apexpay.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Helper component for handling payment race condition recovery.
 * <p>
 * This class exists to solve the problem of recovering from DataIntegrityViolationException
 * when concurrent payment creation occurs. When such an exception occurs within a @Transactional
 * method, the Hibernate Session becomes corrupted and marked for rollback. Subsequent database
 * operations in the same transaction will fail unpredictably.
 * </p>
 * <p>
 * By using {@code REQUIRES_NEW} propagation, recovery operations run in a fresh, independent
 * transaction with a clean Hibernate Session.
 * </p>
 */
@Component
public class PaymentRecoveryHelper {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRecoveryHelper.class);

    private final PaymentRepository paymentRepository;

    /**
     * Constructs a new PaymentRecoveryHelper with the required repository.
     *
     * @param paymentRepository the repository for payment operations
     */
    public PaymentRecoveryHelper(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Recovers from a race condition where concurrent payment creation occurred.
     * <p>
     * Uses {@code REQUIRES_NEW} propagation to ensure this operation runs in its own
     * independent transaction with a fresh Hibernate Session. This is critical because
     * when DataIntegrityViolationException occurs, the original transaction's Session
     * is corrupted and marked for rollback.
     * </p>
     *
     * @param clientRequestId the client request ID that caused the collision
     * @param userUuid        the user's UUID
     * @param request         the original payment request (used if resetting expired payment)
     * @return the existing or reset payment entity
     * @throws BusinessException if the existing payment cannot be found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payments recoverFromRaceCondition(String clientRequestId, UUID userUuid, InitiatePaymentRequest request) {
        logger.info("Recovering from concurrent payment creation. ClientRequestId: {}", clientRequestId);

        Payments existing = paymentRepository
                .findByClientRequestIdAndUserId(clientRequestId, userUuid)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        ErrorMessages.PAYMENT_CREATION_FAILED));

        // If payment is expired, reset it for reuse
        if (existing.getStatus() == PaymentStatusEnum.EXPIRED) {
            return resetExpiredPayment(existing, request);
        }

        return existing;
    }

    /**
     * Resets an expired payment for reuse with new request data.
     * <p>
     * Clears all provider-related fields (providerTransactionId, provider, walletTransactionId,
     * failureCode, failureMessage) and resets status to INITIATED. This allows the same
     * clientRequestId to be reused after a payment has expired.
     * </p>
     *
     * @param existing the expired payment entity to reset
     * @param request  the new payment request with updated details
     * @return the reset and saved payment entity
     */
    private Payments resetExpiredPayment(Payments existing, InitiatePaymentRequest request) {
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
        return paymentRepository.save(existing);
    }
}
