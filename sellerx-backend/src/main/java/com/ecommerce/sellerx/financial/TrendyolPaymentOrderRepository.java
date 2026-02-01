package com.ecommerce.sellerx.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolPaymentOrderRepository extends JpaRepository<TrendyolPaymentOrder, UUID> {

    List<TrendyolPaymentOrder> findByStoreIdOrderByPaymentDateDesc(UUID storeId);

    List<TrendyolPaymentOrder> findByStoreIdAndPaymentDateBetweenOrderByPaymentDateDesc(
            UUID storeId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<TrendyolPaymentOrder> findByStoreIdAndPaymentOrderId(UUID storeId, Long paymentOrderId);

    List<TrendyolPaymentOrder> findByStoreIdAndDiscrepancyStatus(UUID storeId, String discrepancyStatus);

    @Query("SELECT p FROM TrendyolPaymentOrder p " +
            "WHERE p.store.id = :storeId " +
            "AND p.discrepancyStatus IN ('UNDERPAID', 'OVERPAID') " +
            "ORDER BY p.paymentDate DESC")
    List<TrendyolPaymentOrder> findDiscrepanciesByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(p) FROM TrendyolPaymentOrder p " +
            "WHERE p.store.id = :storeId AND p.discrepancyStatus = :status")
    long countByStoreIdAndDiscrepancyStatus(
            @Param("storeId") UUID storeId,
            @Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM TrendyolPaymentOrder p " +
            "WHERE p.store.id = :storeId " +
            "AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.discrepancyAmount), 0) FROM TrendyolPaymentOrder p " +
            "WHERE p.store.id = :storeId " +
            "AND p.paymentDate BETWEEN :startDate AND :endDate " +
            "AND p.discrepancyStatus IN ('UNDERPAID', 'OVERPAID')")
    BigDecimal sumDiscrepancyAmountByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByStoreIdAndPaymentOrderId(UUID storeId, Long paymentOrderId);

    @Query("SELECT p FROM TrendyolPaymentOrder p " +
            "WHERE p.store.id = :storeId " +
            "AND p.discrepancyStatus = 'PENDING' " +
            "ORDER BY p.createdAt ASC")
    List<TrendyolPaymentOrder> findPendingVerificationsByStoreId(@Param("storeId") UUID storeId);
}
