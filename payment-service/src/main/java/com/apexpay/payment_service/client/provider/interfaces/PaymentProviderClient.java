package com.apexpay.payment_service.client.provider.interfaces;

import com.apexpay.payment_service.client.provider.dto.ProviderChargeRequest;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeResponse;

/**
 * Interface for payment provider clients.
 * <p>
 * Defines the contract for interacting with external payment providers
 * (e.g., Stripe, PayPal, or mock providers). Implementations handle the
 * provider-specific details of charging payment methods and querying
 * transaction status.
 * </p>
 */
public interface PaymentProviderClient {
    /**
     * Charges a payment method through the provider.
     * <p>
     * Sends a charge request to the payment provider and returns the result.
     * The response indicates success, failure, or pending status along with
     * provider-specific details.
     * </p>
     *
     * @param request the charge request containing payment details
     * @return response indicating success, failure, or pending with transaction details
     * @throws com.apexpay.payment_service.client.provider.exception.PaymentProviderException
     *         if the provider encounters an error during processing
     */
    ProviderChargeResponse charge(ProviderChargeRequest request);

    /**
     * Retrieves the current status of a previously processed transaction.
     * <p>
     * Queries the provider for the current state of a transaction identified
     * by the provider transaction ID. Useful for checking pending transactions
     * or reconciling payment states.
     * </p>
     *
     * @param providerTransactionId the provider's transaction identifier
     * @return response with current transaction status
     */
    ProviderChargeResponse getTransactionStatus(String providerTransactionId);

    /**
     * Returns the name of this payment provider.
     * <p>
     * Used for logging, identification, and provider-specific logic.
     * Examples: "MOCK", "STRIPE", "PAYPAL"
     * </p>
     *
     * @return the provider name as a string
     */
    String getProviderName();
}
