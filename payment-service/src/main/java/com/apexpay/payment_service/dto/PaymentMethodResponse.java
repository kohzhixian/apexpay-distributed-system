package com.apexpay.payment_service.dto;

import com.apexpay.payment_service.entity.PaymentMethodType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment method information.
 * <p>
 * Contains all relevant fields for displaying a payment method in the UI.
 * Sensitive data (full card numbers) is never included.
 * </p>
 *
 * @param id           unique identifier of the payment method
 * @param type         type of payment method (CARD or BANK_ACCOUNT)
 * @param displayName  user-friendly name, e.g., "Visa ending in 4242"
 * @param last4        last 4 digits of card/account number
 * @param brand        card brand (visa, mastercard, etc.) - null for bank accounts
 * @param expiryMonth  card expiry month - null for bank accounts
 * @param expiryYear   card expiry year - null for bank accounts
 * @param bankName     bank name - null for cards
 * @param accountType  account type (checking/savings) - null for cards
 * @param lastUsedAt   when this payment method was last used
 * @param isDefault    true if this is the most recently used (default) method
 */
public record PaymentMethodResponse(
        UUID id,
        PaymentMethodType type,
        String displayName,
        String last4,
        String brand,
        Integer expiryMonth,
        Integer expiryYear,
        String bankName,
        String accountType,
        Instant lastUsedAt,
        boolean isDefault
) {
}
