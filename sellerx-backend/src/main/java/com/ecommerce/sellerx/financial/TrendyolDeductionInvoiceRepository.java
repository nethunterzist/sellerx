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

/**
 * Repository for TrendyolDeductionInvoice entity.
 * Provides methods to query deduction invoices by store, type, and date range.
 */
@Repository
public interface TrendyolDeductionInvoiceRepository extends JpaRepository<TrendyolDeductionInvoice, UUID> {

    /**
     * Find invoice by store and Trendyol ID
     */
    Optional<TrendyolDeductionInvoice> findByStoreIdAndTrendyolId(UUID storeId, String trendyolId);

    /**
     * Check if invoice exists
     */
    boolean existsByStoreIdAndTrendyolId(UUID storeId, String trendyolId);

    /**
     * Find all invoices for a store in date range
     */
    List<TrendyolDeductionInvoice> findByStoreIdAndTransactionDateBetween(
            UUID storeId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find invoices by type in date range
     */
    List<TrendyolDeductionInvoice> findByStoreIdAndTransactionTypeAndTransactionDateBetween(
            UUID storeId, String transactionType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find invoices by multiple types in date range
     */
    @Query("SELECT d FROM TrendyolDeductionInvoice d WHERE d.storeId = :storeId " +
            "AND d.transactionType IN :types " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate")
    List<TrendyolDeductionInvoice> findByStoreIdAndTypesInDateRange(
            @Param("storeId") UUID storeId,
            @Param("types") List<String> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total debt (amount) for platform fees in date range.
     * Platform fees include: Platform Hizmet Bedeli, Uluslararası Hizmet Bedeli,
     * AZ-Platform Hizmet Bedeli, AZ-Uluslararası Hizmet Bedeli, Yurt Dışı Operasyon Bedeli,
     * Komisyon Faturası, Kusurlu Ürün Faturası, Erken Ödeme Kesinti Faturası
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType IN ('Platform Hizmet Bedeli', 'Uluslararası Hizmet Bedeli', " +
            "'AZ-Platform Hizmet Bedeli', 'AZ-Uluslararası Hizmet Bedeli', 'Yurt Dışı Operasyon Bedeli', " +
            "'Komisyon Faturası', 'Kusurlu Ürün Faturası', 'Erken Ödeme Kesinti Faturası')")
    BigDecimal sumPlatformFeesByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total cargo fees in date range
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType = 'Kargo Fatura'")
    BigDecimal sumCargoFeesByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total advertising fees in date range
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType = 'Reklam Bedeli'")
    BigDecimal sumAdvertisingFeesByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total early payment fees in date range
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType = 'Erken Ödeme Kesinti Faturası'")
    BigDecimal sumEarlyPaymentFeesByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get breakdown of all deduction types with totals for a date range
     */
    @Query("SELECT d.transactionType AS transactionType, COALESCE(SUM(d.debt), 0) AS totalDebt FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.transactionType")
    List<DeductionBreakdownProjection> getDeductionBreakdownByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sum debt by specific transaction type
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType = :transactionType")
    BigDecimal sumByTransactionType(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("transactionType") String transactionType);

    /**
     * Sum other platform fees (excluding main categories and commission invoices).
     * Commission is already calculated from orders, so we exclude 'Komisyon Faturası' to prevent double-counting.
     * Kargo and Reklam are also excluded as they have separate tracking.
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType NOT IN ('Platform Hizmet Bedeli', 'Uluslararası Hizmet Bedeli', " +
            "'AZ-Platform Hizmet Bedeli', 'AZ-Uluslararası Hizmet Bedeli', 'Yurt Dışı Operasyon Bedeli', " +
            "'Kargo Fatura', 'Reklam Bedeli', 'Komisyon Faturası')")
    BigDecimal sumOtherDeductionFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find invoices by store and invoice serial number
     * Used to get all items within a specific invoice
     */
    List<TrendyolDeductionInvoice> findByStoreIdAndInvoiceSerialNumberOrderByTransactionDateDesc(
            UUID storeId, String invoiceSerialNumber);

    /**
     * Count invoices by store and invoice serial number
     */
    long countByStoreIdAndInvoiceSerialNumber(UUID storeId, String invoiceSerialNumber);

    /**
     * Delete all invoices for a store
     */
    void deleteAllByStoreId(UUID storeId);

    /**
     * Count invoices for a store
     */
    long countByStoreId(UUID storeId);

    /**
     * Find invoices by transaction types with pagination.
     * Used for category-level commission/other items listing with lazy loading.
     */
    @Query("SELECT d FROM TrendyolDeductionInvoice d WHERE d.storeId = :storeId " +
            "AND d.transactionType IN :types " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY d.transactionDate DESC")
    Page<TrendyolDeductionInvoice> findByStoreIdAndTypesInDateRangePaged(
            @Param("storeId") UUID storeId,
            @Param("types") List<String> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ============== KESİLEN FATURALAR (Dashboard Kartları için) ==============
    // Komisyon ve Kargo HARİÇ - bunlar sipariş bazlı hesaplanıyor

    /**
     * Sum all advertising fees (Reklam Bedeli + Influencer reklamları)
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType IN ('Reklam Bedeli', 'Sabit Bütçeli Influencer Reklam Bedeli', 'Komisyonlu İnfluencer Reklam Bedeli')")
    BigDecimal sumInvoicedAdvertisingFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sum all penalty fees (Tedarik Edememe, Termin Gecikme, Kusurlu/Eksik/Yanlış Ürün)
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (d.transactionType IN ('Tedarik Edememe', 'TEDARIK EDEMEME FATURASI', 'Termin Gecikme Bedeli', " +
            "'Yanlış Ürün Faturası', 'YANLIS URUN FATURASI', 'Kusurlu Ürün Faturası', 'Eksik Ürün Faturası'))")
    BigDecimal sumInvoicedPenaltyFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sum all international fees (Uluslararası Hizmet, AZ-Uluslararası, Yurtdışı Operasyon)
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (d.transactionType IN ('Uluslararası Hizmet Bedeli', 'AZ-Uluslararası Hizmet Bedeli', " +
            "'AZ-Yurtdışı Operasyon Bedeli', 'AZ-YURTDÕ_Õ OPERASYON BEDELI %18') " +
            "OR d.transactionType LIKE '%Uluslararası%' OR d.transactionType LIKE '%Yurtdışı%')")
    BigDecimal sumInvoicedInternationalFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sum all other fees (Erken Ödeme, Fatura Kontör, etc.)
     * Excludes: Komisyon, Kargo, Reklam, Ceza, Uluslararası, İade, Platform Hizmet (shown individually)
     */
    @Query("SELECT COALESCE(SUM(d.debt), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND d.transactionType NOT IN ('Komisyon Faturası', 'Kargo Fatura', " +
            "'Platform Hizmet Bedeli', 'AZ-Platform Hizmet Bedeli', " +
            "'Reklam Bedeli', 'Sabit Bütçeli Influencer Reklam Bedeli', 'Komisyonlu İnfluencer Reklam Bedeli', " +
            "'Tedarik Edememe', 'TEDARIK EDEMEME FATURASI', 'Termin Gecikme Bedeli', " +
            "'Yanlış Ürün Faturası', 'YANLIS URUN FATURASI', 'Kusurlu Ürün Faturası', 'Eksik Ürün Faturası', " +
            "'Uluslararası Hizmet Bedeli', 'AZ-Uluslararası Hizmet Bedeli', 'AZ-Yurtdışı Operasyon Bedeli', " +
            "'AZ-YURTDÕ_Õ OPERASYON BEDELI %18', 'Yurtdışı Operasyon Iade Bedeli', " +
            "'MP Kargo İtiraz İade Faturası', 'Tazmin Faturası') " +
            "AND d.transactionType NOT LIKE '%Uluslararası%' AND d.transactionType NOT LIKE '%Yurtdışı%' " +
            "AND d.transactionType NOT LIKE '%Iade%' AND d.transactionType NOT LIKE '%İade%'")
    BigDecimal sumInvoicedOtherFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sum all refund credits (İade/Tazmin faturalari - seller'a geri ödeme)
     */
    @Query("SELECT COALESCE(SUM(d.credit), 0) FROM TrendyolDeductionInvoice d " +
            "WHERE d.storeId = :storeId " +
            "AND d.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (d.transactionType IN ('Yurtdışı Operasyon Iade Bedeli', 'MP Kargo İtiraz İade Faturası', 'Tazmin Faturası') " +
            "OR d.transactionType LIKE '%Iade%' OR d.transactionType LIKE '%İade%' OR d.transactionType LIKE '%Tazmin%')")
    BigDecimal sumInvoicedRefunds(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate deduction invoice data by barcode for specific transaction types.
     * Note: Barcode comes from linked order, so we join with orders table.
     * Returns: [barcode, totalQuantity, totalAmount, totalVatAmount, invoiceCount]
     * Used for "Ürünler" tab in KOMISYON category view.
     */
    @Query(value = """
            SELECT
                o.barcode as barcode,
                COUNT(DISTINCT d.id) as totalQuantity,
                COALESCE(SUM(d.debt), 0) as totalAmount,
                COALESCE(SUM(d.debt * 0.20 / 1.20), 0) as totalVatAmount,
                COUNT(DISTINCT d.invoice_serial_number) as invoiceCount
            FROM trendyol_deduction_invoices d
            LEFT JOIN LATERAL (
                SELECT item->>'barcode' as barcode
                FROM trendyol_orders o,
                LATERAL jsonb_array_elements(o.order_items) item
                WHERE o.ty_order_number = d.order_number
                AND o.store_id = d.store_id
                LIMIT 1
            ) o ON true
            WHERE d.store_id = :storeId
            AND d.transaction_type IN :types
            AND d.transaction_date BETWEEN :startDate AND :endDate
            AND o.barcode IS NOT NULL
            GROUP BY o.barcode
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<DeductionByBarcodeProjection> aggregateByBarcodeAndTypesInDateRange(
            @Param("storeId") UUID storeId,
            @Param("types") List<String> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
