package com.apexpay.common.constants;

/**
 * Centralized validation messages used across all services.
 * Ensures consistency in error messaging for validation failures.
 */
public final class ValidationMessages {
    private ValidationMessages() {
    } // Prevent instantiation

    // Amount validations
    public static final String AMOUNT_REQUIRED = "Amount is required.";
    public static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be more than 0.";
    public static final String DECIMAL_MIN_AMOUNT = "0.00";
    public static final String DECIMAL_MIN_BALANCE = "0.0";

    // Balance validations
    public static final String BALANCE_REQUIRED = "Balance is required.";
    public static final String BALANCE_MUST_BE_NON_NEGATIVE = "Balance must be at least zero.";

    // ID validations
    public static final String WALLET_ID_REQUIRED = "Wallet id is required.";
    public static final String USER_ID_REQUIRED = "User id is required.";
    public static final String PAYMENT_ID_REQUIRED = "Payment id is required.";
    public static final String RESERVATION_ID_REQUIRED = "Reservation id is required.";
    public static final String CLIENT_REQUEST_ID_REQUIRED = "Client request id is required.";

    // Transfer validations
    public static final String RECEIVER_USER_ID_REQUIRED = "Receiver user id is required.";
    public static final String RECEIVER_WALLET_ID_REQUIRED = "Receiver wallet id is required.";
    public static final String PAYER_WALLET_ID_REQUIRED = "Payer wallet id is required.";

    // Payment validations
    public static final String CURRENCY_REQUIRED = "Currency is required.";
    public static final String PROVIDER_REQUIRED = "Provider is required.";
    public static final String REFERENCE_REQUIRED = "Reference is required.";
    public static final String PROVIDER_TRANSACTION_ID_REQUIRED = "Provider transaction id is required.";
    public static final String EXTERNAL_PROVIDER_REQUIRED = "External Provider is required.";
    public static final String PAYMENT_METHOD_TOKEN_REQUIRED = "Payment method token is required.";

    // Status validations
    public static final String EXPECTED_STATUS_REQUIRED = "Expected status is required.";
    public static final String TARGET_STATUS_REQUIRED = "Target status is required.";

    // User validations
    public static final String USERNAME_PATTERN = "Username can only contain letters, numbers, and underscores.";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters.";

    // Wallet validations
    public static final String WALLET_NAME_REQUIRED = "Wallet name is required.";
    public static final String WALLET_NAME_MAX_LENGTH = "Wallet name must not exceed 50 characters.";
}
