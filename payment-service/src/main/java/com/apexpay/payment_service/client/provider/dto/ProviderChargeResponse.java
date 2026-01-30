package com.apexpay.payment_service.client.provider.dto;

import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.client.provider.enums.ProviderTransactionStatus;

import java.time.Instant;

/**
 * Response DTO from payment provider charge operations.
 * <p>
 * Contains the result of a charge request, including success/failure status,
 * transaction identifiers, and failure details if applicable. The isRetryable
 * flag indicates whether the operation can be safely retried.
 * </p>
 *
 * @param providerTransactionId the provider's transaction identifier (null if failed before charge)
 * @param status                the transaction status (SUCCESS, PENDING, or FAILED)
 * @param providerName          the name of the payment provider
 * @param failureCode           the provider-specific failure code (null if successful)
 * @param failureMessage        human-readable failure description (null if successful)
 * @param isRetryable           whether the failure can be retried (true for transient errors)
 * @param processedAt           timestamp when the provider processed the request
 */
public record ProviderChargeResponse(
                String providerTransactionId,
                ProviderTransactionStatus status,
                String providerName,
                ProviderFailureCode failureCode, // failure code like "card_declined", "insufficient funds"
                String failureMessage,
                boolean isRetryable,
                Instant processedAt) {

        /**
         * Creates a successful charge response.
         * <p>
         * Used when the payment provider successfully charged the payment method.
         * </p>
         *
         * @param providerTransactionId the provider's transaction identifier
         * @param providerName          the name of the payment provider
         * @return a successful ProviderChargeResponse
         */
        public static ProviderChargeResponse success(
                        String providerTransactionId,
                        String providerName) {
                return new ProviderChargeResponse(providerTransactionId, ProviderTransactionStatus.SUCCESS,
                                providerName,
                                null, null, false, Instant.now());
        }

        /**
         * Creates a failed charge response.
         * <p>
         * Used when the charge failed before reaching the provider (validation error)
         * or when the provider declined the charge. The providerTransactionId is null
         * because no transaction was created.
         * </p>
         *
         * @param providerName   the name of the payment provider
         * @param failureCode    the provider-specific failure code
         * @param failureMessage human-readable failure description
         * @param isRetryable    whether this failure can be retried
         * @return a failed ProviderChargeResponse
         */
        public static ProviderChargeResponse failure(
                        String providerName,
                        ProviderFailureCode failureCode,
                        String failureMessage,
                        boolean isRetryable) {
                return new ProviderChargeResponse(null, ProviderTransactionStatus.FAILED, providerName, failureCode,
                                failureMessage, isRetryable, Instant.now());
        }

        /**
         * Creates a pending charge response.
         * <p>
         * Used when the charge was submitted but the provider has not yet confirmed
         * the final status. The transaction ID is available for status queries.
         * </p>
         *
         * @param providerTransactionId the provider's transaction identifier
         * @param providerName          the name of the payment provider
         * @return a pending ProviderChargeResponse
         */
        public static ProviderChargeResponse pending(
                        String providerTransactionId,
                        String providerName) {
                return new ProviderChargeResponse(providerTransactionId, ProviderTransactionStatus.PENDING,
                                providerName, null,
                                null, false, Instant.now());
        }

        /**
         * Checks if the charge was successful.
         *
         * @return true if status is SUCCESS, false otherwise
         */
        public boolean isSuccessful() {
                return status == ProviderTransactionStatus.SUCCESS;
        }

        /**
         * Checks if the charge failed.
         *
         * @return true if status is FAILED, false otherwise
         */
        public boolean isFailed() {
                return status == ProviderTransactionStatus.FAILED;
        }

}
