package com.apexpay.common.exception;

import lombok.Getter;

/**
 * Base exception for all business logic errors.
 * Uses ErrorCode enum for consistent error handling across services.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Constructs a BusinessException with the default message from ErrorCode.
     *
     * @param errorCode the error code defining the type and default message
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Constructs a BusinessException with a custom message.
     *
     * @param errorCode     the error code defining the type of error
     * @param customMessage custom message overriding the default ErrorCode message
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
