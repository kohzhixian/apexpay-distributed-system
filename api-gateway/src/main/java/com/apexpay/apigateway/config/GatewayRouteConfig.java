package com.apexpay.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route configuration for Spring Cloud Gateway.
 * Defines routes to microservices with circuit breaker patterns for resilience.
 * Supports both Eureka-based (lb://) and direct URL routing for production deployments.
 */
@Configuration
public class GatewayRouteConfig {

    @Value("${apexpay.services.user-service-url:lb://userservice}")
    private String userServiceUrl;

    @Value("${apexpay.services.wallet-service-url:lb://walletservice}")
    private String walletServiceUrl;

    @Value("${apexpay.services.payment-service-url:lb://paymentservice}")
    private String paymentServiceUrl;

    /**
     * Defines custom routes for the API Gateway.
     * Each route includes circuit breaker configuration for resilience.
     * Service URLs are configurable via properties for production deployment without Eureka.
     *
     * @param builder the RouteLocatorBuilder for creating routes
     * @return configured RouteLocator with all service routes
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for User service - Auth, User, and Contacts endpoints
                .route("user-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/user/**", "/api/v1/contacts/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userserviceCB")
                                        .setFallbackUri("forward:/user-fallback")))
                        .uri(userServiceUrl))

                .route("test", r -> r
                        .path("/api/v1/test")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userserviceCB")
                                        .setFallbackUri("forward:/user-fallback")))
                        .uri(userServiceUrl))

                .route("wallet-service", r -> r
                        .path("/api/v1/wallet/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("walletserviceCB")
                                        .setFallbackUri("forward:/wallet-fallback")))
                        .uri(walletServiceUrl))

                .route("payment-service", r -> r
                        .path("/api/v1/payment/**", "/api/v1/payment-methods/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("paymentserviceCB")
                                        .setFallbackUri("forward:/payment-fallback")))
                        .uri(paymentServiceUrl))
                .build();
    }
}
