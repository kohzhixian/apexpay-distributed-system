package com.apexpay.apigateway.config;

import com.apexpay.apigateway.exception.UnauthorizedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * Custom error attributes handler for the API Gateway.
 * Maps specific exceptions to appropriate HTTP status codes:
 * - UnauthorizedException/AuthenticationException → 401 UNAUTHORIZED
 * - Service discovery failures/connection errors → 503 SERVICE_UNAVAILABLE
 * Provides consistent error response format across all gateway errors.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorAttributes.class);

    /**
     * Builds error attributes map for the error response.
     * Maps exceptions to appropriate HTTP status codes and messages.
     *
     * @param request the current server request
     * @param options options for error attribute inclusion
     * @return map of error attributes including status, error, message, path, and timestamp
     */
    @NotNull
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        // Log the actual exception for debugging
        logger.error("Exception caught for path {}: {} - {}",
                request.path(),
                error.getClass().getName(),
                error.getMessage(),
                error);

        if (error instanceof UnauthorizedException
                || error instanceof org.springframework.security.core.AuthenticationException) {
            map.put("status", HttpStatus.UNAUTHORIZED.value());
            map.put("error", "UNAUTHORIZED");
            map.put("message", error.getMessage());
        } else if (isServiceUnavailableError(error)) {
            map.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            map.put("error", "SERVICE_UNAVAILABLE");
            map.put("message", "The requested service is currently unavailable");
        }
        map.put("path", request.path());
        map.put("timestamp", Instant.now().toString());

        return map;
    }

    /**
     * Determines if the error indicates a service unavailability condition.
     * Checks for service discovery failures, connection errors, and load balancer issues.
     *
     * @param error the throwable to analyze
     * @return true if the error indicates service unavailability, false otherwise
     */
    private boolean isServiceUnavailableError(Throwable error) {
        // Check if it's a ResponseStatusException with 404 (service not found scenario)
        if (error instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            // Treat 404 from gateway routing as service unavailable
            if (status == HttpStatus.NOT_FOUND || status == HttpStatus.SERVICE_UNAVAILABLE) {
                return true;
            }
        }

        String errorClassName = error.getClass().getName();
        String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

        // Also check the cause
        Throwable cause = error.getCause();
        String causeClassName = cause != null ? cause.getClass().getName() : "";
        String causeMessage = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";

        return errorClassName.contains("ServiceInstance")
                || errorClassName.contains("UnknownHostException")
                || errorClassName.contains("ConnectException")
                || errorClassName.contains("ServiceUnavailable")
                || errorClassName.contains("NotFoundException")
                || errorClassName.contains("LoadBalancer")
                || causeClassName.contains("ServiceInstance")
                || causeClassName.contains("LoadBalancer")
                || errorMessage.contains("unable to find instance")
                || errorMessage.contains("connection refused")
                || errorMessage.contains("no servers available")
                || errorMessage.contains("did not find any instance")
                || errorMessage.contains("503")
                || errorMessage.contains("service unavailable")
                || causeMessage.contains("unable to find instance")
                || causeMessage.contains("no servers available");
    }
}
