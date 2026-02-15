package com.ecommerce.sellerx.financial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface TrendyolInvoiceRepository extends JpaRepository<TrendyolInvoice, UUID> {

    Optional<TrendyolInvoice> findByStoreIdAndInvoiceNumber(UUID storeId, String invoiceNumber);

    boolean existsByStoreIdAndInvoiceNumber(UUID storeId, String invoiceNumber);

    List<TrendyolInvoice> findByStoreIdAndInvoiceDateBetween(
            UUID storeId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    Page<TrendyolInvoice> findByStoreIdAndInvoiceTypeCode(
            UUID storeId,
            String invoiceTypeCode,
            Pageable pageable
    );

    Page<TrendyolInvoice> findByStoreIdAndInvoiceTypeCodeAndInvoiceDateBetween(
            UUID storeId,
            String invoiceTypeCode,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<TrendyolInvoice> findByStoreIdAndInvoiceCategory(
            UUID storeId,
            String invoiceCategory,
            Pageable pageable
    );

    Page<TrendyolInvoice> findByStoreIdAndInvoiceCategoryAndInvoiceDateBetween(
            UUID storeId,
            String invoiceCategory,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
        SELECT ti.invoiceTypeCode AS invoiceTypeCode, ti.invoiceType AS invoiceType,
               ti.invoiceCategory AS invoiceCategory, ti.isDeduction AS isDeduction,
               COUNT(ti) AS invoiceCount, SUM(ti.amount) AS totalAmount, SUM(ti.vatAmount) AS totalVatAmount
        FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
        GROUP BY ti.invoiceTypeCode, ti.invoiceType, ti.invoiceCategory, ti.isDeduction
        ORDER BY SUM(ti.amount) DESC
    """)
    List<InvoiceSummaryByTypeProjection> getInvoiceSummaryByType(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT ti.invoiceCategory AS invoiceCategory,
               COUNT(ti) AS invoiceCount,
               SUM(ti.amount) AS totalAmount,
               SUM(ti.vatAmount) AS totalVatAmount
        FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
        GROUP BY ti.invoiceCategory
        ORDER BY SUM(ti.amount) DESC
    """)
    List<InvoiceSummaryByCategoryProjection> getInvoiceSummaryByCategory(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT SUM(CASE WHEN ti.isDeduction = true THEN ti.amount ELSE 0 END) AS totalDeductions,
               SUM(CASE WHEN ti.isDeduction = false THEN ti.amount ELSE 0 END) AS totalRefunds,
               COUNT(ti) AS invoiceCount
        FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
    """)
    DeductionAndRefundTotalsProjection getTotalDeductionsAndRefunds(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT ti FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
        ORDER BY ti.invoiceDate DESC, ti.amount DESC
    """)
    Page<TrendyolInvoice> findAllByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByStoreIdAndInvoiceTypeCodeAndInvoiceDateBetween(
            UUID storeId,
            String invoiceTypeCode,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
        SELECT SUM(ti.amount)
        FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceTypeCode = :invoiceTypeCode
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumAmountByStoreIdAndTypeCode(
            @Param("storeId") UUID storeId,
            @Param("invoiceTypeCode") String invoiceTypeCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Sum amounts by invoice category for dashboard "Kesilen Faturalar" calculation.
     * Categories: KOMISYON, KARGO, ULUSLARARASI, CEZA, REKLAM, DIGER, IADE
     */
    @Query("""
        SELECT COALESCE(SUM(ti.amount), 0)
        FROM TrendyolInvoice ti
        WHERE ti.storeId = :storeId
          AND ti.invoiceCategory = :category
          AND ti.invoiceDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumAmountByStoreIdAndCategory(
            @Param("storeId") UUID storeId,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    void deleteByStoreId(UUID storeId);

    /**
     * Find all invoices for a store, ordered by invoice date descending.
     */
    List<TrendyolInvoice> findByStoreIdOrderByInvoiceDateDesc(UUID storeId);
}
