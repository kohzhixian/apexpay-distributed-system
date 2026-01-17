package com.apexpay.wallet_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry for wallet service operations.
 * Used with @Retryable annotations for optimistic lock retry handling.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
