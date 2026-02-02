package com.apexpay.common.constants;

/**
 * Centralized response messages for successful operations.
 */
public final class ResponseMessages {
    // Wallet responses
    public static final String WALLET_CREATED = "Wallet created successfully.";
    public static final String WALLET_TOPUP_SUCCESS = "Wallet top up successfully.";
    public static final String WALLET_NAME_UPDATED = "Wallet name updated successfully.";
    public static final String TRANSFER_SUCCESS = "Transfer made successfully.";
    public static final String RESERVATION_COMPLETED = "Wallet Reservation completed.";
    public static final String RESERVATION_ALREADY_COMPLETED = "Wallet Reservation already completed.";
    public static final String RESERVATION_CANCELLED = "Wallet Reservation cancelled.";
    public static final String RESERVATION_ALREADY_CANCELLED = "Wallet Reservation already cancelled.";
    // Payment responses
    public static final String PAYMENT_INITIATED = "Payment initiated.";
    public static final String RETURNING_EXISTING_PAYMENT = "Returning existing payment.";
    public static final String PAYMENT_SUCCEEDED = "Payment succeeded.";
    public static final String PAYMENT_PENDING = "Payment is pending. Status will be updated once confirmed by provider.";
    // Auth responses
    public static final String REGISTRATION_SUCCESS = "Registration was completed successfully.";
    public static final String LOGIN_SUCCESS = "Login Success.";
    public static final String TOKEN_REFRESHED = "Token refreshed successfully.";
    public static final String LOGOUT_SUCCESS = "Logout Success.";
    
    private ResponseMessages() {
    } // Prevent instantiation
}
