package com.apexpay.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Standardized error response DTO for all ApexPay services.
 * Provides consistent error format across the distributed system.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final Integer code;
    private final String error;
    private final String message;
    private final String path;

    /**
     * Constructs an error response with an application-specific error code.
     * Used for BusinessException and other custom exceptions.
     *
     * @param status  HTTP status code (e.g., 400, 404, 500)
     * @param code    application-specific error code from ErrorCode enum
     * @param error   HTTP status reason phrase (e.g., "Bad Request")
     * @param message detailed error message for debugging
     * @param path    request path that caused the error
     */
    public ErrorResponse(int status, int code, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Constructs an error response without an application-specific error code.
     * Used for Spring's built-in exceptions (validation, type mismatch, etc.).
     *
     * @param status  HTTP status code (e.g., 400, 404, 500)
     * @param error   HTTP status reason phrase (e.g., "Bad Request")
     * @param message detailed error message for debugging
     * @param path    request path that caused the error
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = null;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
