package com.ecommerce.sellerx.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset tokens.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a valid (not expired, not used) token by its value.
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findValidToken(String token);

    /**
     * Find token by its value (regardless of validity).
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalidate all existing tokens for a user (mark as expired by setting expiresAt to now).
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.expiresAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP")
    void invalidateAllTokensForUser(Long userId);

    /**
     * Delete expired tokens older than the given date (for cleanup job).
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
    int deleteExpiredTokensBefore(OffsetDateTime before);

    /**
     * Check if user has a recent valid token (to prevent spam).
     */
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t WHERE t.user.id = :userId AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP AND t.createdAt > :since")
    boolean hasRecentToken(Long userId, OffsetDateTime since);
}
