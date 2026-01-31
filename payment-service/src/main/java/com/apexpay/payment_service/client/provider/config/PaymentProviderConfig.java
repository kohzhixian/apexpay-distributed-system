package com.apexpay.payment_service.client.provider.config;

import com.apexpay.payment_service.client.provider.dto.MockConfig;
import com.apexpay.payment_service.client.provider.interfaces.MockPaymentProviderClient;
import com.apexpay.payment_service.client.provider.interfaces.PaymentProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for payment provider clients.
 * <p>
 * This class is responsible for creating and configuring payment provider
 * client beans
 * based on application properties. It supports conditional bean creation for
 * different
 * payment provider implementations (e.g., mock, Stripe).
 * </p>
 *
 * @author ApexPay
 */
@Configuration
public class PaymentProviderConfig {
    private static final Logger logger = LoggerFactory.getLogger(PaymentProviderConfig.class);

    /**
     * Creates a mock payment provider client bean.
     * <p>
     * This bean is conditionally created when {@code payment.provider.mock.enabled}
     * is set to {@code true} or when the property is missing (defaults to enabled).
     * The mock provider simulates payment processing with configurable success rate
     * and latency to facilitate testing and development.
     * </p>
     *
     * @param properties the payment provider properties containing mock
     *                   configuration
     * @return a configured {@link PaymentProviderClient} implementation (mock)
     */
    @Bean
    @ConditionalOnProperty(name = "payment.provider.mock.enabled", havingValue = "true", matchIfMissing = true)
    public PaymentProviderClient mockPaymentProviderClient(PaymentProviderProperties properties) {
        PaymentProviderProperties.MockProperties mockProperties = properties.getMock();

        logger.info("Initializing MOCK payment provider (success-rate: {}%, latency: {}---{}ms)",
                mockProperties.getSuccessRate() * 100,
                mockProperties.getMinLatencyMs(),
                mockProperties.getMaxLatencyMs());

        MockConfig config = new MockConfig(
                mockProperties.getSuccessRate(),
                mockProperties.getMinLatencyMs(),
                mockProperties.getMaxLatencyMs());

        return new MockPaymentProviderClient(config, mockProperties.getTestTokenOutcomes());
    }
}
