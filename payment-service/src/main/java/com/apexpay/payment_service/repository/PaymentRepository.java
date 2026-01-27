package com.apexpay.payment_service.repository;

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
     * @param userId the user ID who owns the payment
     * @return optional payment if found, empty otherwise
     */
    Optional<Payments> findByClientRequestIdAndUserId(String clientRequestId, UUID userId);

    /**
     * Finds a payment by payment ID and user ID.
     * <p>
     * Ensures the payment belongs to the specified user for authorization checks.
     * </p>
     *
     * @param paymentId the payment ID
     * @param userId the user ID who should own the payment
     * @return optional payment if found and belongs to user, empty otherwise
     */
    Optional<Payments> findByIdAndUserId(UUID paymentId, UUID userId);

    /**
     * Updates payment to SUCCESS status with optimistic locking.
     * <p>
     * Atomically updates payment status, external transaction ID, and version
     * only if the version matches (prevents concurrent modification conflicts).
     * Returns the number of rows updated (0 if version mismatch).
     * </p>
     *
     * @param paymentId the payment ID to update
     * @param externalTransactionId the provider's transaction identifier
     * @param version the expected current version (for optimistic locking)
     * @return number of rows updated (1 if successful, 0 if version mismatch)
     */
    @Modifying
    @Query("UPDATE Payments p SET p.status = 'SUCCESS', p.externalTransactionId = :externalTransactionId, p.version = p.version + 1 WHERE p.id = :paymentId AND p.version = :version")
    int updatePaymentSuccess(@Param("paymentId") UUID paymentId, @Param("externalTransactionId") String externalTransactionId, @Param("version") Long version);

    /**
     * Updates payment to FAILED status with optimistic locking.
     * <p>
     * Atomically updates payment status, failure code, failure message, and version
     * only if the version matches. Returns the number of rows updated.
     * </p>
     *
     * @param paymentId the payment ID to update
     * @param failureCode the provider-specific failure code
     * @param failureMessage human-readable failure description
     * @param version the expected current version (for optimistic locking)
     * @return number of rows updated (1 if successful, 0 if version mismatch)
     */
    @Modifying
    @Query("UPDATE Payments p SET p.status = 'FAILED', p.failureCode = :failureCode, p.failureMessage = :failureMessage, p.version = p.version + 1 WHERE p.id = :paymentId AND p.version = :version")
    int updatePaymentFailed(@Param("paymentId") UUID paymentId, @Param("failureCode") String failureCode,
                            @Param("failureMessage") String failureMessage, @Param("version") Long version);

    /**
     * Updates the external transaction ID for a payment.
     * <p>
     * Used to store the provider's transaction identifier after a successful charge.
     * This method does not use optimistic locking and should be used with caution.
     * </p>
     *
     * @param paymentId the payment ID to update
     * @param externalTransactionId the provider's transaction identifier
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE Payments p SET p.externalTransactionId = :externalTransactionId WHERE p.id = :paymentId")
    int updateExternalTransactionId(@Param("paymentId") UUID paymentId, @Param("externalTransactionId") String externalTransactionId);

}
