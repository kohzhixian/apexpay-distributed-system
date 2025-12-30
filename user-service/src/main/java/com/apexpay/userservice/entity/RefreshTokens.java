package com.apexpay.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a refresh token for JWT authentication.
 * Supports token rotation with family tracking to detect token reuse attacks.
 * Each token belongs to a family (familyId) to enable cascade revocation on suspicious activity.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens", schema = "userservice")
public class RefreshTokens {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    /** BCrypt-hashed refresh token for secure storage */
    @Column(name = "hashed_refresh_token", nullable = false)
    private String hashedRefreshToken;

    /** Groups related tokens for cascade revocation on reuse detection */
    @Builder.Default
    @Column(columnDefinition = "UUID")
    private UUID familyId = UUID.randomUUID();

    /** Marks token as used; reuse of consumed token triggers family revocation */
    @Builder.Default
    private boolean consumed = false;

    /** Client IP for anomaly detection */
    @Column(nullable = false)
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private Instant expiryDate;

    /** Manual revocation flag (e.g., logout, security concern) */
    @Builder.Default
    private boolean isRevoked = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    private Instant updatedDate;
}
