package com.apexpay.payment_service.client.provider.exception;

import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import lombok.Getter;

/**
 * Exception thrown when a payment provider encounters an error.
 * <p>
 * Contains provider-specific failure information including the failure code,
 * whether the operation can be retried, and the provider name. Used to
 * distinguish between retryable transient errors (network issues, timeouts)
 * and non-retryable errors (card declined, invalid card).
 * </p>
 */
@Getter
public class PaymentProviderException extends RuntimeException {
    /** The provider-specific failure code */
    private final ProviderFailureCode failureCode;
    /** Whether this error can be safely retried */
    private final boolean isRetryable;
    /** The name of the payment provider that threw this exception */
    private final String providerName;

    /**
     * Constructs a new PaymentProviderException.
     *
     * @param message      the error message
     * @param failureCode the provider-specific failure code
     * @param isRetryable  whether this error can be retried
     * @param providerName the name of the payment provider
     */
    public PaymentProviderException(String message, ProviderFailureCode failureCode, boolean isRetryable, String providerName) {
        super(message);
        this.failureCode = failureCode;
        this.isRetryable = isRetryable;
        this.providerName = providerName;
    }
}
