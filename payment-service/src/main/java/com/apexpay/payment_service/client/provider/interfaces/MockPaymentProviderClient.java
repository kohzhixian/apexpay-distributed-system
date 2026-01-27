package com.apexpay.payment_service.client.provider.interfaces;

import com.apexpay.payment_service.client.provider.constants.MockProviderConstants;
import com.apexpay.payment_service.client.provider.dto.MockConfig;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeRequest;
import com.apexpay.payment_service.client.provider.dto.ProviderChargeResponse;
import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import com.apexpay.payment_service.client.provider.helper.MockPaymentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of PaymentProviderClient for testing and development.
 * 
 * <p>Simulates an external payment provider (like Stripe, PayPal) with configurable behavior:
 * <ul>
 *   <li>Configurable success rate for random outcomes</li>
 *   <li>Simulated network latency</li>
 *   <li>Test tokens for deterministic testing (e.g., "tok_success", "tok_decline")</li>
 *   <li>In-memory transaction storage for status queries</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * // Default config (80% success rate)
 * MockPaymentProviderClient client = new MockPaymentProviderClient();
 * 
 * // Custom config
 * MockConfig config = MockConfig.builder().successRate(0.5).build();
 * MockPaymentProviderClient client = new MockPaymentProviderClient(config);
 * </pre>
 */
public class MockPaymentProviderClient implements PaymentProviderClient {
    private static final Logger logger = LoggerFactory.getLogger(MockPaymentProviderClient.class);

    private final MockPaymentHelper mockPaymentHelper;
    private final Map<String, ProviderChargeResponse> transactionStore = new ConcurrentHashMap<>();

    /**
     * Creates a client with default configuration (80% success rate).
     */
    public MockPaymentProviderClient() {
        this(new MockPaymentHelper(MockConfig.defaultConfig()));
    }

    /**
     * Creates a client with custom mock configuration.
     * 
     * @param mockConfig configuration for success rate, latency, etc.
     */
    public MockPaymentProviderClient(MockConfig mockConfig) {
        this(new MockPaymentHelper(mockConfig));
    }

    /**
     * Creates a client with a custom MockPaymentHelper (for testing).
     * 
     * @param mockPaymentHelper helper for mock behavior
     */
    public MockPaymentProviderClient(MockPaymentHelper mockPaymentHelper) {
        this.mockPaymentHelper = mockPaymentHelper;
    }

    /**
     * Simulates charging a payment method.
     * 
     * <p>Behavior:
     * <ol>
     *   <li>Simulates network latency (configurable)</li>
     *   <li>Checks for test tokens (deterministic results for testing)</li>
     *   <li>If no test token, uses random success/failure based on successRate</li>
     *   <li>Stores transaction for later status queries</li>
     * </ol>
     * 
     * <p>Test tokens:
     * <ul>
     *   <li>"tok_success" - Always succeeds</li>
     *   <li>"tok_decline" - Always fails (card declined)</li>
     *   <li>"tok_retry" - Fails with retryable error</li>
     * </ul>
     * 
     * @param request the charge request with amount, currency, payment token
     * @return response indicating success or failure with details
     */
    @Override
    public ProviderChargeResponse charge(ProviderChargeRequest request) {
        // log incoming request
        logger.info("Mock provider processing charge: paymentId={}, amount={} {}", request.paymentId(),
                request.amount(), request.currency());

        // simulate network latency
        mockPaymentHelper.simulateLatency();

        // check for test tokens
        ProviderChargeResponse testResponse = mockPaymentHelper.handleTestToken(request.paymentMethodToken());
        if (testResponse != null) {
            mockPaymentHelper.storeTransaction(testResponse, transactionStore);
            return testResponse;
        }

        // No test token - use random behavior based on successRate
        ProviderChargeResponse response;
        if (mockPaymentHelper.shouldSucceed()) {
            String externalTxId = mockPaymentHelper.generateExternalTransactionId();
            response = ProviderChargeResponse.success(externalTxId, MockProviderConstants.PROVIDER_NAME);
            logger.info("Mock provider charge successfully: externalTxId={}", externalTxId);
        } else {
            response = mockPaymentHelper.generateRandomFailure();
            logger.warn("Mock provider charge failed: code={}, retryable={}", response.failureCode(),
                    response.isRetryable());
        }

        mockPaymentHelper.storeTransaction(response, transactionStore);
        return response;
    }

    /**
     * Retrieves the status of a previously processed transaction.
     * 
     * <p>Looks up the transaction in the in-memory store. In a real provider,
     * this would call the provider's API to get current transaction status.
     * 
     * @param externalTransactionId the provider's transaction ID
     * @return the transaction response, or TRANSACTION_NOT_FOUND failure if not found
     */
    @Override
    public ProviderChargeResponse getTransactionStatus(String externalTransactionId) {
        ProviderChargeResponse response = transactionStore.get(externalTransactionId);

        if (response != null) {
            return response;
        }

        // Transaction not found
        return ProviderChargeResponse.failure(
                MockProviderConstants.PROVIDER_NAME,
                ProviderFailureCode.TRANSACTION_NOT_FOUND,
                String.format(MockProviderConstants.MSG_TRANSACTION_NOT_FOUND, externalTransactionId),
                false);
    }

    /**
     * Returns the provider name for logging and identification.
     * 
     * @return "MOCK" for this implementation
     */
    @Override
    public String getProviderName() {
        return MockProviderConstants.PROVIDER_NAME;
    }

}
