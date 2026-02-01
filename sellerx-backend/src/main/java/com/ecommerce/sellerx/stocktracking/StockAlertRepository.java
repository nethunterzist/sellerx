package com.ecommerce.sellerx.stocktracking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    /**
     * Find unread alerts for a store
     */
    List<StockAlert> findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(UUID storeId);

    /**
     * Find all alerts for a store (paginated)
     */
    Page<StockAlert> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    /**
     * Find alerts by type for a store
     */
    List<StockAlert> findByStoreIdAndAlertTypeOrderByCreatedAtDesc(UUID storeId, StockAlertType alertType);

    /**
     * Find alerts for a specific tracked product
     */
    List<StockAlert> findByTrackedProductIdOrderByCreatedAtDesc(UUID trackedProductId);

    /**
     * Count unread alerts for a store
     */
    int countByStoreIdAndIsReadFalse(UUID storeId);

    /**
     * Count alerts by type for a store (last 24 hours)
     */
    @Query("""
        SELECT COUNT(a) FROM StockAlert a
        WHERE a.store.id = :storeId
        AND a.alertType = :alertType
        AND a.createdAt > :since
        """)
    int countByStoreAndTypeAndSince(
            @Param("storeId") UUID storeId,
            @Param("alertType") StockAlertType alertType,
            @Param("since") LocalDateTime since);

    /**
     * Mark all alerts as read for a store
     */
    @Modifying
    @Query("""
        UPDATE StockAlert a
        SET a.isRead = true, a.readAt = CURRENT_TIMESTAMP
        WHERE a.store.id = :storeId AND a.isRead = false
        """)
    int markAllAsReadForStore(@Param("storeId") UUID storeId);

    /**
     * Find recent alerts (last N days)
     */
    @Query("""
        SELECT a FROM StockAlert a
        WHERE a.store.id = :storeId
        AND a.createdAt > :since
        ORDER BY a.createdAt DESC
        """)
    List<StockAlert> findRecentAlerts(
            @Param("storeId") UUID storeId,
            @Param("since") LocalDateTime since);

    /**
     * Delete old alerts (cleanup)
     */
    @Modifying
    @Query("DELETE FROM StockAlert a WHERE a.createdAt < :before")
    int deleteOldAlerts(@Param("before") LocalDateTime before);
}
