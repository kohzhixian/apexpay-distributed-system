package com.apexpay.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for ApexPay services.
 * Error code ranges:
 * - 1xxx: Authentication errors
 * - 2xxx: Resource errors
 * - 3xxx: Validation errors
 * - 4xxx: Conflict errors
 * - 5xxx: Authorization errors
 * - 9xxx: Server errors
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
    WALLET_NOT_FOUND(2003, HttpStatus.NOT_FOUND, "Wallet not found"),
    INVALID_TRANSFER(2004, HttpStatus.CONFLICT, "Invalid transfer"),
    PAYMENT_NOT_FOUND(2005, HttpStatus.NOT_FOUND, "Payment record not found"),
    WALLET_TRANSACTION_NOT_FOUND(2006, HttpStatus.NOT_FOUND, "Wallet transaction not found"),
    CONTACT_NOT_FOUND(2007, HttpStatus.NOT_FOUND, "Contact not found"),
    PAYMENT_METHOD_NOT_FOUND(2008, HttpStatus.NOT_FOUND, "Payment method not found"),

    // Validation errors (3xxx)
    VALIDATION_FAILED(3001, HttpStatus.BAD_REQUEST, "Validation failed"),
    INVALID_INPUT(3002, HttpStatus.BAD_REQUEST, "Invalid input"),
    MISSING_REQUIRED_FIELD(3003, HttpStatus.BAD_REQUEST, "Missing required field"),
    INVALID_STATUS_TRANSITION(3004, HttpStatus.BAD_REQUEST, "Invalid payment status transition"),
    INVALID_STATE(3005, HttpStatus.BAD_REQUEST, "Invalid state for this operation"),
    CANNOT_ADD_SELF_AS_CONTACT(3006, HttpStatus.BAD_REQUEST, "Cannot add yourself as a contact"),
    CURRENCY_MISMATCH(3007, HttpStatus.BAD_REQUEST, "Currency mismatch"),

    // Conflict errors (4xxx)
    USERNAME_EXISTS(4001, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_EXISTS(4002, HttpStatus.CONFLICT, "Email already registered"),
    RESOURCE_ALREADY_EXISTS(4003, HttpStatus.CONFLICT, "Resource already exists"),
    WALLET_ALREADY_EXISTS(4004, HttpStatus.CONFLICT, "Wallet already exists"),
    RESERVATION_FAILURE(4005, HttpStatus.CONFLICT, "Failed to reserve funds."),
    UPDATE_PAYMENT_STATUS_FAILURE(4006, HttpStatus.CONFLICT, "Payment status update failed - concurrent modification detected"),
    CONCURRENT_MODIFICATION(4007, HttpStatus.CONFLICT, "Resource was modified by another process"),
    CONTACT_ALREADY_EXISTS(4008, HttpStatus.CONFLICT, "Contact already exists"),

    // Authorization errors (5xxx)
    ACCESS_DENIED(5001, HttpStatus.FORBIDDEN, "Access denied"),
    INSUFFICIENT_PERMISSIONS(5002, HttpStatus.FORBIDDEN, "Insufficient permissions"),
    INSUFFICIENT_BALANCE(5003, HttpStatus.FORBIDDEN, "Insufficient balance"),

    // Payment errors (6xxx)
    PAYMENT_CHARGE_FAILED(6001, HttpStatus.PAYMENT_REQUIRED, "Payment charge failed"),
    PAYMENT_DECLINED(6002, HttpStatus.PAYMENT_REQUIRED, "Payment was declined"),
    PAYMENT_PROVIDER_UNAVAILABLE(6003, HttpStatus.SERVICE_UNAVAILABLE, "Payment provider unavailable"),
    PAYMENT_PROCESSING_ERROR(6004, HttpStatus.BAD_GATEWAY, "Payment processing error"),

    // Server errors (9xxx)
    INTERNAL_ERROR(9001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SERVICE_UNAVAILABLE(9002, HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
