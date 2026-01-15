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

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for User service - Auth endpoints
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
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
                .build();
    }
}
