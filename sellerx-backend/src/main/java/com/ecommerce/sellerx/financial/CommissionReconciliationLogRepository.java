package com.ecommerce.sellerx.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionReconciliationLogRepository extends JpaRepository<CommissionReconciliationLog, Long> {

    /**
     * Find reconciliation log by store and date
     */
    Optional<CommissionReconciliationLog> findByStoreIdAndReconciliationDate(UUID storeId, LocalDate reconciliationDate);

    /**
     * Find all reconciliation logs for a store
     */
    List<CommissionReconciliationLog> findByStoreIdOrderByReconciliationDateDesc(UUID storeId);

    /**
     * Find reconciliation logs for a store within a date range
     */
    @Query("SELECT r FROM CommissionReconciliationLog r WHERE r.store.id = :storeId " +
           "AND r.reconciliationDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.reconciliationDate DESC")
    List<CommissionReconciliationLog> findByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total reconciled orders for a store
     */
    @Query("SELECT COALESCE(SUM(r.totalReconciled), 0) FROM CommissionReconciliationLog r WHERE r.store.id = :storeId")
    Integer sumTotalReconciledByStoreId(@Param("storeId") UUID storeId);

    /**
     * Get total commission difference for a store (shows estimation accuracy)
     */
    @Query("SELECT COALESCE(SUM(r.totalDifference), 0) FROM CommissionReconciliationLog r WHERE r.store.id = :storeId")
    BigDecimal sumTotalDifferenceByStoreId(@Param("storeId") UUID storeId);

    /**
     * Get average accuracy for a store
     */
    @Query("SELECT AVG(r.averageAccuracy) FROM CommissionReconciliationLog r WHERE r.store.id = :storeId AND r.averageAccuracy IS NOT NULL")
    BigDecimal avgAccuracyByStoreId(@Param("storeId") UUID storeId);

    /**
     * Find the latest reconciliation date for a store
     */
    @Query("SELECT MAX(r.reconciliationDate) FROM CommissionReconciliationLog r WHERE r.store.id = :storeId")
    LocalDate findLatestReconciliationDateByStoreId(@Param("storeId") UUID storeId);

    /**
     * Check if reconciliation exists for a store and date
     */
    boolean existsByStoreIdAndReconciliationDate(UUID storeId, LocalDate reconciliationDate);

    /**
     * Delete reconciliation logs older than a specified date
     */
    void deleteByReconciliationDateBefore(LocalDate date);

    /**
     * Get reconciliation summary statistics for a store
     */
    @Query("SELECT COUNT(r) AS logCount, COALESCE(SUM(r.totalReconciled), 0) AS totalReconciled, " +
           "COALESCE(SUM(r.totalEstimated), 0) AS totalEstimated, " +
           "COALESCE(SUM(r.totalReal), 0) AS totalReal, COALESCE(SUM(r.totalDifference), 0) AS totalDifference " +
           "FROM CommissionReconciliationLog r WHERE r.store.id = :storeId")
    ReconciliationSummaryProjection getReconciliationSummaryByStoreId(@Param("storeId") UUID storeId);
}
