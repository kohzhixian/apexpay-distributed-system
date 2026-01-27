package com.apexpay.payment_service.dto;

/**
 * Request DTO for processing a payment.
 * <p>
 * Contains the payment method token (e.g., card token from Stripe) that
 * will be charged by the payment provider.
 * </p>
 *
 * @param paymentMethodToken the token representing the payment method to charge
 */
public record ProcessPaymentRequest(String paymentMethodToken) {
}
