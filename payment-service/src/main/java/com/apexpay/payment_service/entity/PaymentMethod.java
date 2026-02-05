package com.apexpay.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user's saved payment method.
 * <p>
 * Stores payment method information including card details or bank account info.
 * The lastUsedAt field is used to determine the default payment method (most recently used).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "paymentservice", name = "payment_methods",
        indexes = {
                @Index(name = "idx_payment_methods_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_methods_last_used", columnList = "user_id, last_used_at DESC")
        })
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;

    /**
     * Token used by payment provider. For mock: "mock_card_xxx" or "mock_bank_xxx".
     * For real providers (Stripe): tokenized payment method ID.
     */
    @Column(name = "provider_token", nullable = false, length = 100)
    private String providerToken;

    /**
     * User-friendly display name, e.g., "Visa ending in 4242"
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Last 4 digits of card number or account number
     */
    @Column(nullable = false, length = 4)
    private String last4;

    // Card-specific fields
    /**
     * Card brand: visa, mastercard, amex, etc. Null for bank accounts.
     */
    @Column(length = 20)
    private String brand;

    /**
     * Card expiry month (1-12). Null for bank accounts.
     */
    @Column(name = "expiry_month")
    private Integer expiryMonth;

    /**
     * Card expiry year (e.g., 2025). Null for bank accounts.
     */
    @Column(name = "expiry_year")
    private Integer expiryYear;

    // Bank account-specific fields
    /**
     * Bank name, e.g., "Chase Bank". Null for cards.
     */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /**
     * Account type: checking, savings. Null for cards.
     */
    @Column(name = "account_type", length = 20)
    private String accountType;

    /**
     * Timestamp of last use. Used to determine default payment method.
     * Most recently used payment method is considered the default.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
