package com.apexpay.apigateway.config;

import com.apexpay.apigateway.exception.GlobalErrorWebExceptionHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom authentication entry point that delegates exception handling
 * to the global error handler chain instead of returning a direct response.
 * Enables consistent error formatting through {@link GlobalErrorWebExceptionHandler}.
 */
public class ManagedAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    @NullMarked
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        // Simply pass the exception into the reactive chain
        // GlobalWebExceptionHandler will catch this.
        return Mono.error(ex);
    }
}
