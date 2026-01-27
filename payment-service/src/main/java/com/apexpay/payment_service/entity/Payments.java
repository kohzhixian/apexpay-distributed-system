package com.apexpay.payment_service.entity;

import com.apexpay.common.enums.CurrencyEnum;
import com.apexpay.common.enums.PaymentStatusEnum;
import com.apexpay.payment_service.client.provider.enums.ProviderFailureCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a payment transaction.
 * <p>
 * Stores payment information including amount, currency, status, and provider
 * details. Uses optimistic locking via the version field to handle concurrent
 * updates. The client_request_id and user_id combination is unique to ensure
 * idempotency.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "paymentservice", name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"client_request_id", "user_id"},
                        name = "uk_payment_client_request_user")
        },
        indexes = {
                @Index(name = "idx_payments_external_transaction_id", columnList = "external_transaction_id")
        })
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    private CurrencyEnum currency;

    @Column(name = "client_request_id", nullable = false)

    private String clientRequestId;

    @Column(nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum status;

    @Column(length = 50)
    private String provider;

    @Column(nullable = false)
    @Version
    private Long version;

    @Column(nullable = false)
    private UUID walletId;

    @Column(name = "wallet_transaction_id")
    private UUID walletTransactionId;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private ProviderFailureCode failureCode;

    @Column(length = 500)
    private String failureMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    private Instant updatedDate;
}
