package com.apexpay.wallet_service.client;

import com.apexpay.common.constants.HttpHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Feign client for interacting with the payment service.
 * <p>
 * Provides methods for updating payment method usage timestamps.
 * </p>
 */
@FeignClient(value = "paymentservice", url = "${apexpay.services.payment-service-url:}")
public interface PaymentServiceClient {

    /**
     * Marks a payment method as used by updating its lastUsedAt timestamp.
     * <p>
     * Called after successful wallet operations (e.g., top-up) to track
     * which payment method was most recently used for default selection.
     * </p>
     *
     * @param paymentMethodId the payment method ID to mark as used
     * @param userId          the authenticated user's ID
     */
    @PutMapping("/api/v1/payment-methods/{paymentMethodId}/mark-used")
    void markPaymentMethodAsUsed(
            @PathVariable("paymentMethodId") UUID paymentMethodId,
            @RequestHeader(HttpHeaders.X_USER_ID) String userId);
}
