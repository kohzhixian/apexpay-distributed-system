package com.apexpay.payment_service.client.provider.helper;

import com.apexpay.payment_service.client.provider.constants.MockProviderConstants;
import com.apexpay.payment_service.client.provider.dto.MockConfig;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeResponse;
import com.apexpay.payment_service.client.provider.enums.MockTestTokenOutcome;
import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.client.provider.exception.PaymentProviderException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

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
    private final Map<String, Supplier<ProviderChargeResponse>> testTokenHandlers;

    public MockPaymentHelper(MockConfig mockConfig) {
        this.mockConfig = mockConfig;
        this.testTokenHandlers = buildTestTokenHandlers(null);
    }

    public MockPaymentHelper(MockConfig mockConfig, Map<String, MockTestTokenOutcome> testTokenOutcomes) {
        this.mockConfig = mockConfig;
        this.testTokenHandlers = buildTestTokenHandlers(testTokenOutcomes);
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
        if (response.providerTransactionId() != null) {
            transactionStore.put(response.providerTransactionId(), response);
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

        Supplier<ProviderChargeResponse> handler = testTokenHandlers.get(token);
        if (handler == null) {
            return null;
        }
        return handler.get();
    }

    private Map<String, Supplier<ProviderChargeResponse>> buildTestTokenHandlers(
            Map<String, MockTestTokenOutcome> testTokenOutcomes) {
        Map<String, Supplier<ProviderChargeResponse>> handlers = new HashMap<>(Map.ofEntries(
                Map.entry(
                        MockProviderConstants.TOKEN_VISA_SUCCESS,
                        () -> ProviderChargeResponse.success(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME)
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_CARD_DECLINED,
                        () -> ProviderChargeResponse.failure(
                                MockProviderConstants.PROVIDER_NAME,
                                ProviderFailureCode.CARD_DECLINED,
                                MockProviderConstants.MSG_CARD_DECLINED,
                                false
                        )
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_INSUFFICIENT_FUNDS,
                        () -> ProviderChargeResponse.failure(
                                MockProviderConstants.PROVIDER_NAME,
                                ProviderFailureCode.INSUFFICIENT_FUNDS,
                                MockProviderConstants.MSG_INSUFFICIENT_FUNDS,
                                false
                        )
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_EXPIRED_CARD,
                        () -> ProviderChargeResponse.failure(
                                MockProviderConstants.PROVIDER_NAME,
                                ProviderFailureCode.EXPIRED_CARD,
                                MockProviderConstants.MSG_CARD_EXPIRED,
                                false
                        )
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_FRAUD_SUSPECTED,
                        () -> ProviderChargeResponse.failure(
                                MockProviderConstants.PROVIDER_NAME,
                                ProviderFailureCode.FRAUD_SUSPECTED,
                                MockProviderConstants.MSG_FRAUD_SUSPECTED,
                                false
                        )
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_NETWORK_ERROR,
                        () -> {
                            throw new PaymentProviderException(
                                    MockProviderConstants.MSG_NETWORK_TIMEOUT,
                                    ProviderFailureCode.NETWORK_ERROR,
                                    true,
                                    MockProviderConstants.PROVIDER_NAME
                            );
                        }
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_PROVIDER_UNAVAILABLE,
                        () -> {
                            throw new PaymentProviderException(
                                    MockProviderConstants.MSG_PROVIDER_UNAVAILABLE,
                                    ProviderFailureCode.PROVIDER_UNAVAILABLE,
                                    true,
                                    MockProviderConstants.PROVIDER_NAME
                            );
                        }
                ),
                Map.entry(
                        MockProviderConstants.TOKEN_RATE_LIMITED,
                        () -> {
                            throw new PaymentProviderException(
                                    MockProviderConstants.MSG_RATE_LIMITED,
                                    ProviderFailureCode.RATE_LIMITED,
                                    true,
                                    MockProviderConstants.PROVIDER_NAME
                            );
                        }
                ),
                Map.entry(MockProviderConstants.TOKEN_SLOW_RESPONSE, this::handleSlowResponse),
                Map.entry(
                        MockProviderConstants.TOKEN_PENDING,
                        () -> ProviderChargeResponse.pending(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME)
                )
        ));

        if (testTokenOutcomes == null || testTokenOutcomes.isEmpty()) {
            return handlers;
        }

        for (Map.Entry<String, MockTestTokenOutcome> entry : testTokenOutcomes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            handlers.put(entry.getKey(), buildOutcomeHandler(entry.getValue()));
        }

        return handlers;
    }

    private Supplier<ProviderChargeResponse> buildOutcomeHandler(MockTestTokenOutcome outcome) {
        return switch (outcome) {
            case SUCCESS ->
                    () -> ProviderChargeResponse.success(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
            case CARD_DECLINED ->
                    () -> ProviderChargeResponse.failure(
                            MockProviderConstants.PROVIDER_NAME,
                            ProviderFailureCode.CARD_DECLINED,
                            MockProviderConstants.MSG_CARD_DECLINED,
                            false
                    );
            case INSUFFICIENT_FUNDS ->
                    () -> ProviderChargeResponse.failure(
                            MockProviderConstants.PROVIDER_NAME,
                            ProviderFailureCode.INSUFFICIENT_FUNDS,
                            MockProviderConstants.MSG_INSUFFICIENT_FUNDS,
                            false
                    );
            case EXPIRED_CARD ->
                    () -> ProviderChargeResponse.failure(
                            MockProviderConstants.PROVIDER_NAME,
                            ProviderFailureCode.EXPIRED_CARD,
                            MockProviderConstants.MSG_CARD_EXPIRED,
                            false
                    );
            case FRAUD_SUSPECTED ->
                    () -> ProviderChargeResponse.failure(
                            MockProviderConstants.PROVIDER_NAME,
                            ProviderFailureCode.FRAUD_SUSPECTED,
                            MockProviderConstants.MSG_FRAUD_SUSPECTED,
                            false
                    );
            case NETWORK_ERROR ->
                    () -> {
                        throw new PaymentProviderException(
                                MockProviderConstants.MSG_NETWORK_TIMEOUT,
                                ProviderFailureCode.NETWORK_ERROR,
                                true,
                                MockProviderConstants.PROVIDER_NAME
                        );
                    };
            case PROVIDER_UNAVAILABLE ->
                    () -> {
                        throw new PaymentProviderException(
                                MockProviderConstants.MSG_PROVIDER_UNAVAILABLE,
                                ProviderFailureCode.PROVIDER_UNAVAILABLE,
                                true,
                                MockProviderConstants.PROVIDER_NAME
                        );
                    };
            case RATE_LIMITED ->
                    () -> {
                        throw new PaymentProviderException(
                                MockProviderConstants.MSG_RATE_LIMITED,
                                ProviderFailureCode.RATE_LIMITED,
                                true,
                                MockProviderConstants.PROVIDER_NAME
                        );
                    };
            case SLOW_RESPONSE -> this::handleSlowResponse;
            case PENDING ->
                    () -> ProviderChargeResponse.pending(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
        };
    }

    private ProviderChargeResponse handleSlowResponse() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ProviderChargeResponse.success(generateExternalTransactionId(), MockProviderConstants.PROVIDER_NAME);
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
