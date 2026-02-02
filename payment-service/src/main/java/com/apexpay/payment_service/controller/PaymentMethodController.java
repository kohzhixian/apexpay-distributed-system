package com.apexpay.payment_service.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.payment_service.dto.PaymentMethodResponse;
import com.apexpay.payment_service.service.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment method operations.
 * <p>
 * Handles listing and deleting user payment methods.
 * All endpoints require authentication via the X-USER-ID header.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    /**
     * Lists all payment methods for the authenticated user.
     * <p>
     * Returns payment methods ordered by last used timestamp (most recent first).
     * The first payment method in the list is marked as the default.
     * If the user has no payment methods, mock payment methods are auto-created
     * for demo purposes.
     * </p>
     *
     * @param userId the authenticated user's ID from the X-USER-ID header
     * @return list of payment methods, default first
     */
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods(
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        List<PaymentMethodResponse> paymentMethods = paymentMethodService.getUserPaymentMethods(userId);
        return ResponseEntity.ok(paymentMethods);
    }

    /**
     * Deletes a payment method for the authenticated user.
     * <p>
     * Removes the specified payment method from the user's saved methods.
     * Only the owner of the payment method can delete it.
     * </p>
     *
     * @param paymentMethodId the ID of the payment method to delete
     * @param userId          the authenticated user's ID from the X-USER-ID header
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable("paymentMethodId") UUID paymentMethodId,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        paymentMethodService.deletePaymentMethod(paymentMethodId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks a payment method as recently used.
     * <p>
     * Updates the lastUsedAt timestamp to the current time, which affects
     * the default payment method selection (most recently used = default).
     * Only the owner of the payment method can mark it as used.
     * </p>
     *
     * @param paymentMethodId the ID of the payment method to mark as used
     * @param userId          the authenticated user's ID from the X-USER-ID header
     * @return 204 No Content on successful update
     */
    @PutMapping("/{paymentMethodId}/mark-used")
    public ResponseEntity<Void> markPaymentMethodAsUsed(
            @PathVariable("paymentMethodId") UUID paymentMethodId,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId) {
        paymentMethodService.markAsUsed(paymentMethodId, userId);
        return ResponseEntity.noContent().build();
    }
}
