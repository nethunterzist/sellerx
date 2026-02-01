package com.ecommerce.sellerx.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    /**
     * Find by event ID - used for idempotency check
     */
    Optional<WebhookEvent> findByEventId(String eventId);

    /**
     * Check if event exists - faster than full fetch
     */
    boolean existsByEventId(String eventId);

    /**
     * Get events for a store (paginated, newest first)
     */
    Page<WebhookEvent> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    /**
     * Get events by type for a store
     */
    Page<WebhookEvent> findByStoreIdAndEventTypeOrderByCreatedAtDesc(
            UUID storeId, String eventType, Pageable pageable);

    /**
     * Get events within date range
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.storeId = :storeId " +
           "AND w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    Page<WebhookEvent> findByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Count events by processing status for a store (for dashboard statistics)
     */
    @Query("SELECT w.processingStatus AS processingStatus, COUNT(w) AS statusCount FROM WebhookEvent w " +
           "WHERE w.storeId = :storeId GROUP BY w.processingStatus")
    List<StatusCountProjection> countByStoreIdGroupByStatus(@Param("storeId") UUID storeId);

    /**
     * Get recent failed events (for monitoring)
     */
    Page<WebhookEvent> findByProcessingStatusOrderByCreatedAtDesc(
            WebhookEvent.ProcessingStatus status, Pageable pageable);

    /**
     * Count total events for a store
     */
    long countByStoreId(UUID storeId);
}
