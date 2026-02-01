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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, UUID> {

    /**
     * Find latest snapshot for a tracked product
     */
    Optional<StockSnapshot> findFirstByTrackedProductIdOrderByCheckedAtDesc(UUID trackedProductId);

    /**
     * Find snapshots for a product (paginated, newest first)
     */
    Page<StockSnapshot> findByTrackedProductIdOrderByCheckedAtDesc(UUID trackedProductId, Pageable pageable);

    /**
     * Find snapshots within a date range
     */
    List<StockSnapshot> findByTrackedProductIdAndCheckedAtBetweenOrderByCheckedAtAsc(
            UUID trackedProductId, LocalDateTime start, LocalDateTime end);

    /**
     * Find recent snapshots (last 30 days)
     */
    @Query("""
        SELECT s FROM StockSnapshot s
        WHERE s.trackedProduct.id = :productId
        AND s.checkedAt > :since
        ORDER BY s.checkedAt DESC
        """)
    List<StockSnapshot> findRecentSnapshots(
            @Param("productId") UUID productId,
            @Param("since") LocalDateTime since);

    /**
     * Delete old snapshots (cleanup job)
     */
    @Modifying
    @Query("""
        DELETE FROM StockSnapshot s
        WHERE s.trackedProduct.id = :productId
        AND s.checkedAt < :before
        """)
    int deleteOldSnapshots(@Param("productId") UUID productId, @Param("before") LocalDateTime before);

    /**
     * Count snapshots for a product
     */
    long countByTrackedProductId(UUID trackedProductId);
}
