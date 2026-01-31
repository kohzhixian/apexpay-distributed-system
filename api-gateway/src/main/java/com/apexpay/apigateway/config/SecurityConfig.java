package com.apexpay.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * WebFlux security configuration for the API Gateway.
 * Disables default Spring Security features (CSRF, form login, HTTP Basic)
 * as the gateway is stateless and uses JWT for authentication.
 * All authorization checks are delegated to {@link GatewayJwtFilter}.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final String allowedOrigin;

    public SecurityConfig(@Value("${apexpay.cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    public GatewayJwtFilter gatewayJwtFilter(
            @Value("${apexpay.jwt.public-key}") Resource publicKey
    ) {
        return new GatewayJwtFilter(publicKey);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, GatewayJwtFilter gatewayJwtFilter) {
        return http
                // Enable CORS - uses corsConfigurationSource bean
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF - Gateway is stateless and uses JWT
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Disable form login - not needed for API gateway
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Disable HTTP Basic - using JWT instead
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Disable logout - Handled in user service
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .addFilterAt(gatewayJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // Allows all requests - GatewayJwtFilter handles authentication
                .authorizeExchange(exchanges -> exchanges.anyExchange()
                        .permitAll())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
