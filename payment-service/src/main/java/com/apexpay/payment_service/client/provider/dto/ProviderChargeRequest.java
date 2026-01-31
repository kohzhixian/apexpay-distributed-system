package com.apexpay.payment_service.client.provider.dto;

import com.apexpay.common.enums.CurrencyEnum;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for charging a payment method through a payment provider.
 * <p>
 * Contains all information needed to process a charge request with an external
 * payment provider. The idempotencyKey ensures that duplicate requests are
 * handled safely by the provider.
 * </p>
 *
 * @param paymentId         the internal payment ID
 * @param amount            the amount to charge
 * @param currency          the currency of the charge
 * @param paymentMethodToken the token representing the payment method (e.g., card token)
 * @param description       a description of the charge for records
 * @param idempotencyKey    a unique key to ensure idempotency (typically the paymentId)
 */
public record ProviderChargeRequest(
        UUID paymentId,
        BigDecimal amount,
        CurrencyEnum currency,
        String paymentMethodToken,
        String description,
        String idempotencyKey // Using String for flexibility - can allow ids like "payment_abc123_retry_2"
) {

    /**
     * Creates a ProviderChargeRequest with default idempotency key.
     * <p>
     * Uses the paymentId as the idempotency key, which is sufficient for
     * most use cases.
     * </p>
     *
     * @param paymentId         the internal payment ID
     * @param amount            the amount to charge
     * @param currency          the currency of the charge
     * @param paymentMethodToken the token representing the payment method
     * @param description       a description of the charge
     * @return a new ProviderChargeRequest instance
     */
    public static ProviderChargeRequest of(
            UUID paymentId,
            BigDecimal amount,
            CurrencyEnum currency,
            String paymentMethodToken,
            String description
    ) {
        return new ProviderChargeRequest(paymentId, amount, currency, paymentMethodToken, description, paymentId.toString());
    }
}
