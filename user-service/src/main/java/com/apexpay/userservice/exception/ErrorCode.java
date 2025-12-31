package com.apexpay.userservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for the ApexPay User Service.
 * Error code ranges:
 * - 1xxx: Authentication errors
 * - 2xxx: Resource errors
 * - 3xxx: Validation errors
 * - 4xxx: Conflict errors
 * - 5xxx: Server errors
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication errors (1xxx)
    INVALID_CREDENTIALS(1001, HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    TOKEN_EXPIRED(1002, HttpStatus.UNAUTHORIZED, "Token has expired"),
    TOKEN_INVALID(1003, HttpStatus.UNAUTHORIZED, "Invalid token"),
    TOKEN_MISSING(1004, HttpStatus.UNAUTHORIZED, "Token not provided"),
    TOKEN_REUSE_DETECTED(1005, HttpStatus.UNAUTHORIZED, "Token reuse detected"),
    AUTHENTICATION_FAILED(1006, HttpStatus.UNAUTHORIZED, "Authentication failed"),

    // Resource errors (2xxx)
    USER_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "User not found"),
    RESOURCE_NOT_FOUND(2002, HttpStatus.NOT_FOUND, "Resource not found"),

    // Validation errors (3xxx)
    VALIDATION_FAILED(3001, HttpStatus.BAD_REQUEST, "Validation failed"),
    INVALID_INPUT(3002, HttpStatus.BAD_REQUEST, "Invalid input"),
    MISSING_REQUIRED_FIELD(3003, HttpStatus.BAD_REQUEST, "Missing required field"),

    // Conflict errors (4xxx)
    USERNAME_EXISTS(4001, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_EXISTS(4002, HttpStatus.CONFLICT, "Email already registered"),
    RESOURCE_ALREADY_EXISTS(4003, HttpStatus.CONFLICT, "Resource already exists"),

    // Authorization errors (5xxx)
    ACCESS_DENIED(5001, HttpStatus.FORBIDDEN, "Access denied"),
    INSUFFICIENT_PERMISSIONS(5002, HttpStatus.FORBIDDEN, "Insufficient permissions"),

    // Server errors (9xxx)
    INTERNAL_ERROR(9001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
