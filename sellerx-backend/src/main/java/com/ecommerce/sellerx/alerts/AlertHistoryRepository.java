package com.ecommerce.sellerx.alerts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AlertHistory entity.
 * Provides methods for querying triggered alert history.
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {

    /**
     * Find all alerts for a user, ordered by creation date.
     */
    Page<AlertHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find unread alerts for a user.
     */
    List<AlertHistory> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    /**
     * Find alerts for a specific store.
     */
    Page<AlertHistory> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    /**
     * Find alerts by type for a user.
     */
    Page<AlertHistory> findByUserIdAndAlertTypeOrderByCreatedAtDesc(
            Long userId, AlertType alertType, Pageable pageable);

    /**
     * Find alerts by severity for a user.
     */
    Page<AlertHistory> findByUserIdAndSeverityOrderByCreatedAtDesc(
            Long userId, AlertSeverity severity, Pageable pageable);

    /**
     * Find a specific alert by ID and user ID (security check).
     */
    Optional<AlertHistory> findByIdAndUserId(UUID id, Long userId);

    /**
     * Count unread alerts for a user.
     */
    long countByUserIdAndReadAtIsNull(Long userId);

    /**
     * Count alerts by type for a user.
     */
    long countByUserIdAndAlertType(Long userId, AlertType alertType);

    /**
     * Mark all alerts as read for a user.
     */
    @Modifying
    @Query("UPDATE AlertHistory a SET a.readAt = CURRENT_TIMESTAMP WHERE a.user.id = :userId AND a.readAt IS NULL")
    int markAllAsReadForUser(@Param("userId") Long userId);

    /**
     * Find recent alerts for a user (last 24 hours).
     */
    @Query("SELECT a FROM AlertHistory a WHERE a.user.id = :userId " +
           "AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AlertHistory> findRecentAlerts(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Find alerts that need email sending.
     */
    @Query("SELECT a FROM AlertHistory a WHERE a.emailSent = false " +
           "AND a.createdAt >= :since ORDER BY a.createdAt ASC")
    List<AlertHistory> findAlertsNeedingEmail(@Param("since") LocalDateTime since);

    /**
     * Delete old alerts (for cleanup job).
     */
    @Modifying
    @Query("DELETE FROM AlertHistory a WHERE a.createdAt < :cutoffDate")
    int deleteOldAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find alerts created within a date range for a user.
     */
    @Query("SELECT a FROM AlertHistory a WHERE a.user.id = :userId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AlertHistory> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count alerts by date for statistics.
     */
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.user.id = :userId " +
           "AND a.createdAt >= :since AND a.alertType = :alertType")
    long countByUserAndTypeAndSince(
            @Param("userId") Long userId,
            @Param("alertType") AlertType alertType,
            @Param("since") LocalDateTime since);
}
