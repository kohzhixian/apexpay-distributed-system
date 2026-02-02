package com.apexpay.payment_service.repository;

import com.apexpay.payment_service.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PaymentMethod entities.
 * <p>
 * Provides data access methods for payment method records.
 * </p>
 */
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Finds all payment methods for a user, ordered by last used timestamp descending.
     * The first result is the default payment method (most recently used).
     *
     * @param userId the user ID
     * @return list of payment methods, most recently used first
     */
    List<PaymentMethod> findByUserIdOrderByLastUsedAtDesc(UUID userId);

    /**
     * Finds a specific payment method by ID and user ID.
     * Used to verify ownership before operations.
     *
     * @param id     the payment method ID
     * @param userId the user ID
     * @return optional payment method if found and owned by user
     */
    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a user has any payment methods.
     *
     * @param userId the user ID
     * @return true if user has at least one payment method
     */
    boolean existsByUserId(UUID userId);

    /**
     * Updates the lastUsedAt timestamp for a payment method.
     * Called after a successful payment to update the default.
     *
     * @param id         the payment method ID
     * @param lastUsedAt the new timestamp
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.lastUsedAt = :lastUsedAt WHERE pm.id = :id")
    int updateLastUsedAt(@Param("id") UUID id, @Param("lastUsedAt") Instant lastUsedAt);
}
