package com.apexpay.payment_service.client.provider.config;

import com.apexpay.payment_service.client.provider.enums.MockTestTokenOutcome;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for payment provider clients.
 * <p>
 * This class binds payment provider configuration from application properties
 * (with prefix {@code payment.provider}) to Java objects. It supports multiple
 * payment provider implementations including mock and Stripe.
 * </p>
 * <p>
 * Example configuration in application.yaml:
 * <pre>
 * payment:
 *   provider:
 *     mock:
 *       enabled: true
 *       success-rate: 0.95
 *       min-latency-ms: 100
 *       max-latency-ms: 500
 *       test-token-outcomes:
 *         tok_success: SUCCESS
 *         tok_decline: CARD_DECLINED
 *     stripe:
 *       api-key: sk_test_...
 *       webhook-secret: whsec_...
 * </pre>
 * </p>
 *
 * @author ApexPay
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.provider")
public class PaymentProviderProperties {
    /**
     * Configuration properties for the mock payment provider.
     * Used for testing and development purposes.
     */
    private MockProperties mock = new MockProperties();

    /**
     * Configuration properties for the Stripe payment provider.
     * Used for production payment processing.
     */
    private StripeProperties stripe = new StripeProperties();

    /**
     * Configuration properties for the mock payment provider.
     * <p>
     * The mock provider simulates payment processing with configurable
     * success rate and latency ranges for testing scenarios.
     * </p>
     */
    @Getter
    @Setter
    public static class MockProperties {
        /**
         * Whether the mock payment provider is enabled.
         * Defaults to {@code true}.
         */
        private boolean enabled = true;

        /**
         * Success rate for mock payment processing (0.0 to 1.0).
         * A value of 0.95 means 95% of payments will succeed.
         * Defaults to {@code 0.95}.
         */
        private double successRate = 0.95;

        /**
         * Minimum latency in milliseconds for mock payment processing.
         * Defaults to {@code 100} ms.
         */
        private long minLatencyMs = 100;

        /**
         * Maximum latency in milliseconds for mock payment processing.
         * Defaults to {@code 500} ms.
         */
        private long maxLatencyMs = 500;

        /**
         * Deterministic outcomes for test tokens.
         * Keys are token strings; values are {@link com.apexpay.payment_service.client.provider.enums.MockTestTokenOutcome}.
         */
        private Map<String, MockTestTokenOutcome> testTokenOutcomes = new HashMap<>();
    }

    /**
     * Configuration properties for the Stripe payment provider.
     * <p>
     * Contains authentication credentials required to interact with
     * the Stripe payment processing API.
     * </p>
     */
    @Getter
    @Setter
    public static class StripeProperties {
        /**
         * Stripe API key for authenticating API requests.
         * Should be set in application properties (e.g., {@code payment.provider.stripe.api-key}).
         */
        private String apiKey;

        /**
         * Stripe webhook secret for verifying webhook signatures.
         * Used to validate incoming webhook events from Stripe.
         * Should be set in application properties (e.g., {@code payment.provider.stripe.webhook-secret}).
         */
        private String webhookSecret;
    }
}



