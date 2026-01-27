package com.apexpay.wallet_service.entity;

import com.apexpay.wallet_service.enums.ReferenceTypeEnum;
import com.apexpay.wallet_service.enums.TransactionTypeEnum;
import com.apexpay.wallet_service.enums.WalletTransactionStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a wallet transaction.
 * <p>
 * Records all financial movements in a wallet including top-ups, transfers,
 * and payment reservations. Tracks transaction type (CREDIT/DEBIT), status
 * (PENDING/COMPLETED/CANCELLED), and references to related entities (e.g., payment ID).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "walletservice", name = "wallet_transactions")
public class WalletTransactions {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallets wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_type", nullable = false, length = 6)
    @Enumerated(EnumType.STRING)
    private TransactionTypeEnum transactionType;

    private String referenceId;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private ReferenceTypeEnum referenceType;

    private String description;

    @Column(nullable = false, length = 25)
    private WalletTransactionStatusEnum status;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

}
