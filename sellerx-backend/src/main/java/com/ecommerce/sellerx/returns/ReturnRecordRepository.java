package com.ecommerce.sellerx.returns;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, UUID> {

    // Find all returns for a store within date range
    @Query("SELECT r FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.returnDate DESC")
    List<ReturnRecord> findByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count returns for a store within date range
    @Query("SELECT COUNT(r) FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate")
    long countByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Sum total loss for a store within date range
    @Query("SELECT COALESCE(SUM(r.totalLoss), 0) FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumTotalLossByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get top returned products (by return count)
    @Query("SELECT r.barcode AS barcode, r.productName AS productName, COUNT(r) AS returnCount, SUM(r.totalLoss) AS totalLoss " +
           "FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.barcode, r.productName " +
           "ORDER BY returnCount DESC")
    List<TopReturnedProductProjection> findTopReturnedProducts(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get return reason distribution
    @Query("SELECT r.returnReason AS returnReason, COUNT(r) AS reasonCount FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate " +
           "AND r.returnReason IS NOT NULL " +
           "GROUP BY r.returnReason " +
           "ORDER BY COUNT(r) DESC")
    List<ReturnReasonDistributionProjection> findReturnReasonDistribution(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get daily return stats
    @Query("SELECT FUNCTION('DATE', r.returnDate) AS returnDate, COUNT(r) AS returnCount, SUM(r.totalLoss) AS totalLoss " +
           "FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', r.returnDate) " +
           "ORDER BY FUNCTION('DATE', r.returnDate)")
    List<DailyReturnStatsProjection> findDailyReturnStats(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get cost breakdown totals
    @Query("SELECT COALESCE(SUM(r.productCost), 0) AS totalProductCost, " +
           "COALESCE(SUM(r.shippingCostOut), 0) AS totalShippingCostOut, " +
           "COALESCE(SUM(r.shippingCostReturn), 0) AS totalShippingCostReturn, " +
           "COALESCE(SUM(r.commissionLoss), 0) AS totalCommissionLoss, " +
           "COALESCE(SUM(r.packagingCost), 0) AS totalPackagingCost, " +
           "COALESCE(SUM(r.totalLoss), 0) AS totalLoss " +
           "FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.returnDate BETWEEN :startDate AND :endDate")
    ReturnCostBreakdownProjection findCostBreakdownByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find returns by barcode
    List<ReturnRecord> findByStoreIdAndBarcode(UUID storeId, String barcode);

    // Find returns without commission refund (for hakediş kontrolü)
    @Query("SELECT r FROM ReturnRecord r WHERE r.store.id = :storeId " +
           "AND r.commissionRefunded = false " +
           "AND r.returnDate < :cutoffDate " +
           "ORDER BY r.returnDate ASC")
    List<ReturnRecord> findMissingCommissionRefunds(
            @Param("storeId") UUID storeId,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    // Check if return record exists for order and barcode
    boolean existsByOrderIdAndBarcode(UUID orderId, String barcode);

    // Find all returns for a store (used by Sandbox)
    List<ReturnRecord> findByStoreId(UUID storeId);

    // Find return records by order ID
    List<ReturnRecord> findByOrderId(UUID orderId);
}
