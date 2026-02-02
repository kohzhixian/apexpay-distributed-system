package com.apexpay.payment_service.service;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.payment_service.dto.PaymentMethodResponse;
import com.apexpay.payment_service.entity.PaymentMethod;
import com.apexpay.payment_service.entity.PaymentMethodType;
import com.apexpay.payment_service.repository.PaymentMethodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user payment methods.
 * <p>
 * Handles CRUD operations for payment methods and provides
 * auto-seeding of mock payment methods for demo purposes.
 * </p>
 */
@Service
public class PaymentMethodService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentMethodService.class);

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Gets all payment methods for a user, ordered by last used (default first).
     * If user has no payment methods, auto-seeds mock payment methods for demo.
     *
     * @param userId the user ID
     * @return list of payment method responses, default (most recently used) first
     */
    @Transactional
    public List<PaymentMethodResponse> getUserPaymentMethods(String userId) {
        UUID userUuid = UUID.fromString(userId);

        // Check if user has any payment methods, if not, seed mock data
        if (!paymentMethodRepository.existsByUserId(userUuid)) {
            logger.info("No payment methods found for user {}. Seeding mock payment methods.", userId);
            seedMockPaymentMethods(userUuid);
        }

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUserIdOrderByLastUsedAtDesc(userUuid);

        System.out.println("================== PAYMENT METHODS: " + paymentMethods);

        // Map to response DTOs, marking the first one as default
        return mapToResponses(paymentMethods);
    }

    /**
     * Deletes a payment method for a user.
     *
     * @param paymentMethodId the payment method ID to delete
     * @param userId          the user ID (for ownership verification)
     * @throws BusinessException if payment method not found or not owned by user
     */
    @Transactional
    public void deletePaymentMethod(UUID paymentMethodId, String userId) {
        UUID userUuid = UUID.fromString(userId);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userUuid)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PAYMENT_METHOD_NOT_FOUND,
                        ErrorMessages.PAYMENT_METHOD_NOT_FOUND));

        paymentMethodRepository.delete(paymentMethod);
        logger.info("Deleted payment method {} for user {}", paymentMethodId, userId);
    }

    /**
     * Marks a payment method as used by updating its lastUsedAt timestamp.
     * Validates that the payment method belongs to the user before updating.
     *
     * @param paymentMethodId the payment method ID
     * @param userId          the user ID (for ownership verification)
     * @throws BusinessException if payment method not found or not owned by user
     */
    @Transactional
    public void markAsUsed(UUID paymentMethodId, String userId) {
        validatePaymentMethod(paymentMethodId, userId);
        paymentMethodRepository.updateLastUsedAt(paymentMethodId, Instant.now());
        logger.info("Marked payment method {} as used for user {}", paymentMethodId, userId);
    }

    /**
     * Validates that a payment method exists and belongs to the user.
     *
     * @param paymentMethodId the payment method ID
     * @param userId          the user ID
     * @return the payment method if valid
     * @throws BusinessException if not found or not owned by user
     */
    @Transactional(readOnly = true)
    public PaymentMethod validatePaymentMethod(UUID paymentMethodId, String userId) {
        UUID userUuid = UUID.fromString(userId);

        return paymentMethodRepository.findByIdAndUserId(paymentMethodId, userUuid)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PAYMENT_METHOD_NOT_FOUND,
                        ErrorMessages.PAYMENT_METHOD_NOT_FOUND));
    }

    /**
     * Seeds mock payment methods for a user (for demo purposes).
     * Creates one Visa card and one Chase bank account.
     */
    private void seedMockPaymentMethods(UUID userId) {
        Instant now = Instant.now();

        // Create mock Visa card (most recently used)
        PaymentMethod visaCard = PaymentMethod.builder()
                .userId(userId)
                .type(PaymentMethodType.CARD)
                .providerToken("tok_visa_success")
                .displayName("Visa ending in 4242")
                .last4("4242")
                .brand("visa")
                .expiryMonth(9)
                .expiryYear(2025)
                .lastUsedAt(now)
                .build();

        // Create mock bank account (used 1 day ago)
        PaymentMethod bankAccount = PaymentMethod.builder()
                .userId(userId)
                .type(PaymentMethodType.BANK_ACCOUNT)
                .providerToken("mock_bank_chase_9921")
                .displayName("Chase Bank")
                .last4("9921")
                .bankName("Chase Bank")
                .accountType("checking")
                .lastUsedAt(now.minus(1, ChronoUnit.DAYS))
                .build();

        paymentMethodRepository.save(visaCard);
        paymentMethodRepository.save(bankAccount);

        logger.info("Seeded mock payment methods for user {}", userId);
    }

    /**
     * Maps PaymentMethod entities to response DTOs.
     * The first item in the list is marked as the default.
     */
    private List<PaymentMethodResponse> mapToResponses(List<PaymentMethod> paymentMethods) {
        if (paymentMethods.isEmpty()) {
            return List.of();
        }

        // First item is the default (most recently used)
        return paymentMethods.stream()
                .map(pm -> {
                    boolean isDefault = pm.equals(paymentMethods.getFirst());
                    return new PaymentMethodResponse(
                            pm.getId(),
                            pm.getType(),
                            pm.getDisplayName(),
                            pm.getLast4(),
                            pm.getBrand(),
                            pm.getExpiryMonth(),
                            pm.getExpiryYear(),
                            pm.getBankName(),
                            pm.getAccountType(),
                            pm.getLastUsedAt(),
                            isDefault
                    );
                })
                .toList();
    }
}
