package com.apexpay.payment_service.client.provider.helper;

import com.apexpay.payment_service.client.provider.constants.MockProviderConstants;
import com.apexpay.payment_service.client.provider.dto.MockConfig;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeResponse;
import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.client.provider.exception.PaymentProviderException;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Helper class for mock payment provider behavior.
 * <p>
 * Provides utilities for simulating payment provider operations including
 * network latency, success/failure determination, test token handling, and
 * random failure generation. Used by MockPaymentProviderClient to simulate
 * realistic payment provider behavior for testing and development.
 * </p>
 */
public class MockPaymentHelper {
    private final MockConfig mockConfig;
    private final Random random = new Random();

    public MockPaymentHelper(MockConfig mockConfig) {
        this.mockConfig = mockConfig;
    }

    /**
     * Simulates network latency by sleeping for a random duration.
     * <p>
     * Sleeps for a random time between minLatencyMs and maxLatencyMs to
     * simulate real-world network delays. Helps test timeout handling
     * and retry mechanisms.
     * </p>
     */
    public void simulateLatency() {
        long minLatencyMs = mockConfig.minLatencyMs();
        long maxLatencyMs = mockConfig.maxLatencyMs();

        // Only simulate latency if configured
        if (minLatencyMs > 0 || maxLatencyMs > 0) {
            try {
                long latency;
                if (minLatencyMs == maxLatencyMs) {
                    latency = minLatencyMs;
                } else {
                    latency = minLatencyMs + random.nextLong(maxLatencyMs - minLatencyMs + 1);
                }
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Determines if a payment should succeed based on configured success rate.
     *
     * @return true if payment should succeed, false otherwise
     */
    public boolean shouldSucceed() {
        return random.nextDouble() < mockConfig.successRate();
    }

    /**
     * Generates a mock external transaction ID.
     * <p>
     * Creates a provider-like transaction identifier for successful charges.
     * Format: prefix + 24 characters from UUID.
     * </p>
     *
     * @return a mock external transaction ID
     */
    public String generateExternalTransactionId() {
        return MockProviderConstants.TRANSACTION_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * Stores a transaction response in the transaction store.
     * <p>
     * Used to enable status queries for previously processed transactions.
     * </p>
     *
     * @param response        the transaction response to store
     * @param transactionStore the map to store the transaction in
     */
    public void storeTransaction(ProviderChargeResponse response, Map<String, ProviderChargeResponse> transactionStore) {
        if (response.externalTransactionId() != null) {
            transactionStore.put(response.externalTransactionId(), response);
        }
    }

    /**
     * Handles test tokens for deterministic testing scenarios.
     * <p>
     * Returns predefined responses for specific test tokens (e.g., "tok_success",
     * "tok_decline") to enable deterministic testing. Returns null if the token
     * is not a recognized test token.
     * </p>
     *
     * @param token the payment method token to check
     * @return a predefined response if token is a test token, null otherwise
     */
    public ProviderChargeResponse handleTestToken(String token) {
        if (token == null) {
            return null;
        }

        switch (token) {
            case MockProviderConstants.TOKEN_VISA_SUCCESS -> {
                return ProviderChargeResponse.success(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
            }

            case MockProviderConstants.TOKEN_CARD_DECLINED -> {
                return ProviderChargeResponse.failure(
                        MockProviderConstants.PROVIDER_NAME,
                        ProviderFailureCode.CARD_DECLINED,
                        MockProviderConstants.MSG_CARD_DECLINED,
                        false
                );
            }

            case MockProviderConstants.TOKEN_INSUFFICIENT_FUNDS -> {
                return ProviderChargeResponse.failure(
                        MockProviderConstants.PROVIDER_NAME,
                        ProviderFailureCode.INSUFFICIENT_FUNDS,
                        MockProviderConstants.MSG_INSUFFICIENT_FUNDS,
                        false
                );
            }

            case MockProviderConstants.TOKEN_EXPIRED_CARD -> {
                return ProviderChargeResponse.failure(
                        MockProviderConstants.PROVIDER_NAME,
                        ProviderFailureCode.EXPIRED_CARD,
                        MockProviderConstants.MSG_CARD_EXPIRED,
                        false
                );
            }

            case MockProviderConstants.TOKEN_FRAUD_SUSPECTED -> {
                return ProviderChargeResponse.failure(
                        MockProviderConstants.PROVIDER_NAME,
                        ProviderFailureCode.FRAUD_SUSPECTED,
                        MockProviderConstants.MSG_FRAUD_SUSPECTED,
                        false
                );
            }

            case MockProviderConstants.TOKEN_NETWORK_ERROR -> {
                // Throw exception for retryable errors
                throw new PaymentProviderException(
                        MockProviderConstants.MSG_NETWORK_TIMEOUT,
                        ProviderFailureCode.NETWORK_ERROR,
                        true,
                        MockProviderConstants.PROVIDER_NAME
                );
            }

            case MockProviderConstants.TOKEN_PROVIDER_UNAVAILABLE -> {
                throw new PaymentProviderException(
                        MockProviderConstants.MSG_PROVIDER_UNAVAILABLE,
                        ProviderFailureCode.PROVIDER_UNAVAILABLE,
                        true,
                        MockProviderConstants.PROVIDER_NAME
                );
            }

            case MockProviderConstants.TOKEN_RATE_LIMITED -> {
                throw new PaymentProviderException(
                        MockProviderConstants.MSG_RATE_LIMITED,
                        ProviderFailureCode.RATE_LIMITED,
                        true,
                        MockProviderConstants.PROVIDER_NAME
                );
            }

            case MockProviderConstants.TOKEN_SLOW_RESPONSE -> {
                // Simulate very slow provider (5 seconds)
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return ProviderChargeResponse.success(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
            }

            case MockProviderConstants.TOKEN_PENDING -> {
                return ProviderChargeResponse.pending(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
            }

            default -> {
                return null; // Not a test token, proceed with random behavior
            }
        }
    }

    /**
     * Generates a random failure response based on weighted probabilities.
     * <p>
     * Failure distribution:
     * <ul>
     * <li>40% - Card declined (non-retryable)</li>
     * <li>20% - Insufficient funds (non-retryable)</li>
     * <li>20% - Network error (retryable)</li>
     * <li>20% - Provider unavailable (retryable)</li>
     * </ul>
     * </p>
     *
     * @return a ProviderChargeResponse with a randomly selected failure
     */
    public ProviderChargeResponse generateRandomFailure() {
        double roll = random.nextDouble();

        if (roll < 0.4) {
            // 40% - Card declined (non-retryable)
            return ProviderChargeResponse.failure(
                    MockProviderConstants.PROVIDER_NAME,
                    ProviderFailureCode.CARD_DECLINED,
                    MockProviderConstants.MSG_CARD_DECLINED_BY_BANK,
                    false
            );
        } else if (roll < 0.6) {
            // 20% - Insufficient funds (non-retryable)
            return ProviderChargeResponse.failure(
                    MockProviderConstants.PROVIDER_NAME,
                    ProviderFailureCode.INSUFFICIENT_FUNDS,
                    MockProviderConstants.MSG_INSUFFICIENT_FUNDS_SHORT,
                    false
            );
        } else if (roll < 0.8) {
            // 20% - Network error (retryable)
            return ProviderChargeResponse.failure(
                    MockProviderConstants.PROVIDER_NAME,
                    ProviderFailureCode.NETWORK_ERROR,
                    MockProviderConstants.MSG_NETWORK_TIMEOUT_SHORT,
                    true
            );
        } else {
            // 20% - Provider unavailable (retryable)
            return ProviderChargeResponse.failure(
                    MockProviderConstants.PROVIDER_NAME,
                    ProviderFailureCode.PROVIDER_UNAVAILABLE,
                    MockProviderConstants.MSG_SERVICE_UNAVAILABLE,
                    true
            );
        }
    }
}
