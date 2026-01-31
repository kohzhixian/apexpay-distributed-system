package com.apexpay.common.constants;

/**
 * Centralized error messages used across all services.
 * These messages are used with BusinessException to provide consistent error messaging.
 */
public final class ErrorMessages {
    private ErrorMessages() {} // Prevent instantiation

    // User/Auth errors
    public static final String USERNAME_ALREADY_TAKEN = "Username already taken.";
    public static final String EMAIL_ALREADY_REGISTERED = "Email already registered.";
    public static final String INVALID_EMAIL_OR_PASSWORD = "Invalid email or password.";
    public static final String USER_NOT_FOUND = "User not found.";

    // Token errors
    public static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token not found.";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token.";
    public static final String INVALID_REFRESH_TOKEN_FORMAT = "Invalid refresh token format.";
    public static final String REFRESH_TOKEN_EXPIRED_OR_REVOKED = "Refresh token expired or revoked.";
    public static final String REFRESH_TOKEN_VERIFICATION_FAILED = "Refresh token verification failed.";
    public static final String TOKEN_REUSE_DETECTED = "Security alert: Token reuse detected.";

    // Wallet errors
    public static final String WALLET_NOT_FOUND = "Wallet not found.";
    public static final String WALLET_ACCESS_DENIED = "Wallet does not belong to user.";
    public static final String INSUFFICIENT_BALANCE = "Insufficient balance.";
    public static final String CANNOT_TRANSFER_SAME_WALLET = "Cannot transfer to the same wallet.";
    public static final String USER_ID_REQUIRED = "User id is required.";
    public static final String INVALID_USER_ID_FORMAT = "Invalid User id format.";
    public static final String INVALID_WALLET_TRANSACTION_ID = "Invalid Wallet transaction id.";
    public static final String WALLET_TRANSACTION_NOT_FOUND = "Wallet transaction not found.";
    public static final String WALLET_TRANSACTION_MISMATCH = "Wallet transaction does not belong to this wallet.";
    public static final String CONCURRENT_UPDATE_RETRY = "Too many concurrent updates. Please try again.";
    public static final String WALLET_MODIFIED_RETRY = "Wallet was modified. Please try again.";
    public static final String RESERVATION_FAILURE = "Failed to reserve funds.";
    public static final String INVALID_INPUT = "Invalid input.";
    public static final String INVALID_RESERVED_BALANCE_STATE = "Invalid reserved balance state.";
    public static final String INVALID_TRANSACTION_TRANSITION = "Transaction cannot transition to %s. Current status: %s";

    // Payment errors
    public static final String PAYMENT_NOT_FOUND = "Payment record not found.";
    public static final String PAYMENT_ACCESS_DENIED = "Payment does not belong to user.";
    public static final String PAYMENT_INVALID_STATUS = "Payment must be in INITIATED status, current: %s";
    public static final String PAYMENT_CREATION_FAILED = "Unexpected error during payment creation.";
    public static final String PAYMENT_SUCCEEDED = "Payment succeeded.";
    public static final String MAX_RETRIES_EXCEEDED = "Max retries exceeded";
    public static final String MAX_RETRIES_EXCEEDED_WITH_MSG = "Max retries exceeded: %s";
    public static final String UNEXPECTED_ERROR = "Unexpected error: %s";
    public static final String RETRY_INTERRUPTED = "Retry interrupted.";
    public static final String CONCURRENT_PAYMENT_UPDATE = "Payment was modified by another process. Please try again.";

    // Generic errors
    public static final String UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred.";
    public static final String VALIDATION_FAILED = "Validation Failed.";
    public static final String INVALID_PARAMETER_FORMAT = "Invalid format for parameter '%s'. Expected: %s";
}
