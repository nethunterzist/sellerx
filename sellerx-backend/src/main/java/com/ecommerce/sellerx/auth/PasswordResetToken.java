package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for password reset tokens.
 * Tokens are valid for 1 hour and can only be used once.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_tokens")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /**
     * Check if token is still valid (not expired and not used).
     */
    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    /**
     * Check if token has expired.
     */
    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * Check if token has been used.
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Mark token as used.
     */
    public void markAsUsed() {
        this.usedAt = OffsetDateTime.now();
    }
}
