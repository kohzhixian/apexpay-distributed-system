package com.apexpay.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a user's contact for wallet transfers.
 * <p>
 * Stores contact information linked to the owner user for quick access during transfers.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contacts", schema = "userservice",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_user_id", "contact_wallet_id"}))
public class Contacts {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private Users ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private Users contactUser;

    @Column(name = "contact_wallet_id", nullable = false)
    private UUID contactWalletId;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_username", nullable = false)
    private String contactUsername;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    private Instant updatedDate;
}
