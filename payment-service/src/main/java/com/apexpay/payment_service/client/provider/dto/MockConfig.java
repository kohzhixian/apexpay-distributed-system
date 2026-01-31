package com.apexpay.payment_service.client.provider.dto;

/**
 * Configuration for mock payment provider behavior.
 * <p>
 * Controls the success rate and latency simulation for the mock payment
 * provider, allowing configurable testing scenarios.
 * </p>
 *
 * @param successRate  the probability of success (0.0 to 1.0, where 1.0 = 100% success)
 * @param minLatencyMs minimum simulated network latency in milliseconds
 * @param maxLatencyMs maximum simulated network latency in milliseconds
 */
public record MockConfig(double successRate, long minLatencyMs, long maxLatencyMs) {

    /**
     * Returns default configuration (95% success rate, 100-500ms latency).
     *
     * @return default MockConfig instance
     */
    public static MockConfig defaultConfig() {
        return new MockConfig(0.95, 100, 500);
    }

    /**
     * Returns configuration for always succeeding (100% success rate).
     * <p>
     * Useful for unit/integration tests that require deterministic success behavior.
     * </p>
     *
     * @return MockConfig with 100% success rate
     */
    public static MockConfig alwaysSucceed() {
        return new MockConfig(1.0, 50, 100);
    }

    /**
     * Returns configuration for always failing (0% success rate).
     * <p>
     * Useful for unit/integration tests that require deterministic failure behavior.
     * </p>
     *
     * @return MockConfig with 0% success rate
     */
    public static MockConfig alwaysFail() {
        return new MockConfig(0.0, 50, 100);
    }

    /**
     * Returns configuration with no latency simulation.
     * <p>
     * Useful for fast-running unit/integration tests where latency simulation
     * is not needed or would slow down test execution.
     * </p>
     *
     * @return MockConfig with 0ms latency
     */
    public static MockConfig noLatency() {
        return new MockConfig(0.95, 0, 0);
    }
}


