package com.apexpay.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route configuration for Spring Cloud Gateway.
 * Defines routes to microservices with circuit breaker patterns for resilience.
 * Routes are registered with Eureka service discovery using load-balanced URIs.
 */
@Configuration
public class GatewayRouteConfig {

    /**
     * Defines custom routes for the API Gateway.
     * Each route includes circuit breaker configuration for resilience.
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
                        .uri("lb://userservice"))

                .route("test", r -> r
                        .path("/api/v1/test")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userserviceCB")
                                        .setFallbackUri("forward:/user-fallback")))
                        .uri("lb://userservice"))

                .route("wallet-service", r -> r
                        .path("/api/v1/wallet/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("walletserviceCB")
                                        .setFallbackUri("forward:/wallet-fallback")))
                        .uri("lb://walletservice"))

                .route("payment-service", r -> r
                        .path("/api/v1/payment/**", "/api/v1/payment-methods/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("paymentserviceCB")
                                        .setFallbackUri("forward:/payment-fallback")))
                        .uri("lb://paymentservice"))
                .build();
    }
}
