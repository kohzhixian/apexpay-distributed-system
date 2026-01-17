package com.apexpay.common.exception;

import lombok.Getter;

/**
 * Base exception for all business logic errors.
 * Uses ErrorCode enum for consistent error handling across services.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
