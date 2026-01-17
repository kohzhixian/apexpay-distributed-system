package com.apexpay.wallet_service.entity;

import com.apexpay.common.enums.CurrencyEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "walletservice", name = "wallets")
public class Wallets {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedBalance;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyEnum currency;

    @Column(nullable = false)
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    private Instant updatedDate;
}
