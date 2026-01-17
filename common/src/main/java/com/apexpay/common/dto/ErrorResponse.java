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

    /** Constructor with error code (for BusinessException) */
    public ErrorResponse(int status, int code, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /** Constructor without error code (for Spring's built-in exceptions) */
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = null;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
