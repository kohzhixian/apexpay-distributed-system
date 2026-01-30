package com.apexpay.payment_service.repository;

import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payment entities.
 * <p>
 * Provides data access methods for payment records, including custom queries
 * for optimistic locking updates and idempotency checks.
 * </p>
 */
public interface PaymentRepository extends JpaRepository<Payments, UUID> {
    /**
     * Finds a payment by client request ID and user ID.
     * <p>
     * Used for idempotency checks to prevent duplicate payment creation
     * when the same clientRequestId is submitted multiple times.
     * </p>
     *
     * @param clientRequestId the client-provided unique request identifier
     * @param userId          the user ID who owns the payment
     * @return optional payment if found, empty otherwise
     */
    Optional<Payments> findByClientRequestIdAndUserId(String clientRequestId, UUID userId);

    /**
     * Updates payment to SUCCESS status with optimistic locking.
     * <p>
     * Atomically updates payment status, provider transaction ID, and version
     * only if the version matches (prevents concurrent modification conflicts).
     * Returns the number of rows updated (0 if version mismatch).
     * </p>
     *
     * @param paymentId             the payment ID to update
     * @param providerTransactionId the provider's transaction identifier
     * @param version               the expected current version (for optimistic locking)
     * @return number of rows updated (1 if successful, 0 if version mismatch)
     */
    @Modifying
    @Query("UPDATE Payments p SET p.status = 'SUCCESS', p.providerTransactionId = :providerTransactionId, p.version = p.version + 1 WHERE p.id = :paymentId AND p.version = :version")
    int updatePaymentSuccess(@Param("paymentId") UUID paymentId, @Param("providerTransactionId") String providerTransactionId, @Param("version") Long version);

    /**
     * Updates payment to PENDING status with provider transaction ID and wallet transaction ID.
     * Uses optimistic locking to prevent concurrent modification conflicts.
     * <p>
     * Atomically updates payment status, provider transaction ID, provider name, wallet transaction ID,
     * and version only if the version matches (prevents concurrent modification conflicts).
     * Returns the number of rows updated (0 if version mismatch).
     * </p>
     *
     * @param paymentId             the payment ID to update
     * @param providerTransactionId the provider's transaction identifier
     * @param providerName          the name of the payment provider
     * @param walletTransactionId   the wallet transaction ID created during fund reservation
     * @param version               the expected current version (for optimistic locking)
     * @return number of rows updated (1 if successful, 0 if version mismatch)
     */
    @Modifying
    @Query("UPDATE Payments p SET p.status = 'PENDING', p.providerTransactionId = :providerTransactionId, p.provider = :providerName, p.walletTransactionId = :walletTransactionId, p.version = p.version + 1 WHERE p.id = :paymentId AND p.version = :version")
    int updatePaymentPending(@Param("paymentId") UUID paymentId, @Param("providerTransactionId") String providerTransactionId,
                             @Param("providerName") String providerName, @Param("walletTransactionId") UUID walletTransactionId,
                             @Param("version") Long version);

    /**
     * Updates payment to FAILED status with optimistic locking.
     * <p>
     * Atomically updates payment status, failure code, failure message, and version
     * only if the version matches. Returns the number of rows updated.
     * </p>
     *
     * @param paymentId      the payment ID to update
     * @param failureCode    the provider-specific failure code
     * @param failureMessage human-readable failure description
     * @param version        the expected current version (for optimistic locking)
     * @return number of rows updated (1 if successful, 0 if version mismatch)
     */
    @Modifying
    @Query("UPDATE Payments p SET p.status = 'FAILED', p.failureCode = :failureCode, p.failureMessage = :failureMessage, p.version = p.version + 1 WHERE p.id = :paymentId AND p.version = :version")
    int updatePaymentFailed(@Param("paymentId") UUID paymentId, @Param("failureCode") ProviderFailureCode failureCode,
                            @Param("failureMessage") String failureMessage, @Param("version") Long version);

}
