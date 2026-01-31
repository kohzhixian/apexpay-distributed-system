package com.apexpay.apigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when authentication fails at the API Gateway.
 * <p>
 * Used by {@link com.apexpay.apigateway.config.GatewayJwtFilter} when:
 * <ul>
 *   <li>No authentication token is provided for protected endpoints</li>
 *   <li>The provided JWT token is invalid or expired</li>
 * </ul>
 * Always results in HTTP 401 UNAUTHORIZED response.
 * </p>
 */
public class UnauthorizedException extends ResponseStatusException {

    /**
     * Constructs an UnauthorizedException with a custom message.
     *
     * @param message description of why authentication failed
     */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
