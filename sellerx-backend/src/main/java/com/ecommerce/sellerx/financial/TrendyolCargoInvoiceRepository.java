package com.ecommerce.sellerx.financial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolCargoInvoiceRepository extends JpaRepository<TrendyolCargoInvoice, UUID> {

    List<TrendyolCargoInvoice> findByStoreIdOrderByInvoiceDateDesc(UUID storeId);

    List<TrendyolCargoInvoice> findByStoreIdAndInvoiceDateBetweenOrderByInvoiceDateDesc(
            UUID storeId, LocalDate startDate, LocalDate endDate);

    /**
     * Find cargo invoices by store and date range with pagination.
     * Used for category-level cargo items listing with lazy loading.
     */
    Page<TrendyolCargoInvoice> findByStoreIdAndInvoiceDateBetweenOrderByInvoiceDateDescCreatedAtDesc(
            UUID storeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Aggregate cargo invoice data by barcode for a date range.
     * Joins with trendyol_orders to get barcode and productName from order_items.
     * Returns: [barcode, productName, totalQuantity, totalAmount, totalVatAmount, totalDesi, invoiceCount]
     * Used for "Ürünler" tab in KARGO category view.
     */
    @Query(value = """
            SELECT
                item->>'barcode' as barcode,
                MAX(item->>'productName') as productName,
                COUNT(DISTINCT ci.id) as totalQuantity,
                COALESCE(SUM(ci.amount), 0) as totalAmount,
                COALESCE(SUM(ci.vat_amount), 0) as totalVatAmount,
                COALESCE(SUM(ci.desi), 0) as totalDesi,
                COUNT(DISTINCT ci.invoice_serial_number) as invoiceCount
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND item->>'barcode' IS NOT NULL
            GROUP BY item->>'barcode'
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<CargoByBarcodeProjection> aggregateByBarcodeAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(c.vatAmount), 0) FROM TrendyolCargoInvoice c " +
            "WHERE c.store.id = :storeId " +
            "AND c.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumVatByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM TrendyolCargoInvoice c " +
            "WHERE c.store.id = :storeId " +
            "AND c.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum outbound shipping costs ('Gönderi Kargo Bedeli') grouped by order number.
     * Used by ReturnAnalyticsService to get real outbound shipping costs for returned orders.
     */
    @Query(value = """
            SELECT c.order_number as orderNumber, SUM(c.amount) as totalAmount
            FROM trendyol_cargo_invoices c
            WHERE c.store_id = :storeId
              AND c.order_number IN (:orderNumbers)
              AND c.shipment_package_type = 'Gönderi Kargo Bedeli'
            GROUP BY c.order_number
            """, nativeQuery = true)
    List<OrderShippingCostProjection> sumOutboundShippingByOrderNumbers(
            @Param("storeId") UUID storeId,
            @Param("orderNumbers") List<String> orderNumbers);

    /**
     * Sum return shipping costs ('İade Kargo Bedeli') grouped by order number.
     * Used by ReturnAnalyticsService to get real return shipping costs for returned orders.
     */
    @Query(value = """
            SELECT c.order_number as orderNumber, SUM(c.amount) as totalAmount
            FROM trendyol_cargo_invoices c
            WHERE c.store_id = :storeId
              AND c.order_number IN (:orderNumbers)
              AND c.shipment_package_type = 'İade Kargo Bedeli'
            GROUP BY c.order_number
            """, nativeQuery = true)
    List<OrderShippingCostProjection> sumReturnShippingByOrderNumbers(
            @Param("storeId") UUID storeId,
            @Param("orderNumbers") List<String> orderNumbers);

    /**
     * Find distinct order numbers that have return cargo invoices ('İade Kargo Bedeli') in a date range.
     * Used by ReturnAnalyticsService to identify returned orders from cargo invoice data.
     */
    @Query("SELECT DISTINCT c.orderNumber FROM TrendyolCargoInvoice c " +
           "WHERE c.store.id = :storeId " +
           "AND c.shipmentPackageType = 'İade Kargo Bedeli' " +
           "AND c.invoiceDate BETWEEN :startDate AND :endDate")
    List<String> findReturnCargoOrderNumbers(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    Optional<TrendyolCargoInvoice> findByStoreIdAndInvoiceSerialNumberAndShipmentPackageId(
            UUID storeId, String invoiceSerialNumber, Long shipmentPackageId);

    List<TrendyolCargoInvoice> findByStoreIdAndOrderNumber(UUID storeId, String orderNumber);

    List<TrendyolCargoInvoice> findByStoreIdAndShipmentPackageId(UUID storeId, Long shipmentPackageId);

    @Query("SELECT DISTINCT c.invoiceSerialNumber FROM TrendyolCargoInvoice c WHERE c.store.id = :storeId")
    List<String> findDistinctInvoiceSerialNumbersByStoreId(@Param("storeId") UUID storeId);

    boolean existsByStoreIdAndInvoiceSerialNumberAndShipmentPackageId(
            UUID storeId, String invoiceSerialNumber, Long shipmentPackageId);

    /**
     * Find all cargo invoice items by store and invoice serial number.
     * Used to show breakdown of shipments in a cargo invoice.
     */
    List<TrendyolCargoInvoice> findByStoreIdAndInvoiceSerialNumberOrderByInvoiceDateDescCreatedAtDesc(
            UUID storeId, String invoiceSerialNumber);

    /**
     * Find cargo invoice items by store and invoice serial number with pagination.
     * Used for lazy loading in invoice detail panel.
     */
    Page<TrendyolCargoInvoice> findByStoreIdAndInvoiceSerialNumberOrderByInvoiceDateDescCreatedAtDesc(
            UUID storeId, String invoiceSerialNumber, Pageable pageable);

    /**
     * Count items for a specific cargo invoice.
     */
    long countByStoreIdAndInvoiceSerialNumber(UUID storeId, String invoiceSerialNumber);

    /**
     * Find the most recent cargo invoice for a specific barcode.
     * Queries the rawData JSONB field for barcode matching.
     * Used for shipping cost estimation fallback when lastShippingCostPerUnit is NULL.
     */
    @Query(value = """
            SELECT * FROM trendyol_cargo_invoices
            WHERE store_id = :storeId
            AND raw_data->>'barcode' = :barcode
            AND amount > 0
            ORDER BY invoice_date DESC, created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<TrendyolCargoInvoice> findLatestByStoreIdAndBarcode(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode);

    /**
     * Batch find latest cargo invoices for multiple barcodes.
     * More efficient than multiple single queries for shipping estimation.
     * Returns the most recent cargo invoice for each barcode using DISTINCT ON.
     */
    @Query(value = """
            SELECT DISTINCT ON (raw_data->>'barcode') *
            FROM trendyol_cargo_invoices
            WHERE store_id = :storeId
            AND raw_data->>'barcode' IN (:barcodes)
            AND amount > 0
            ORDER BY raw_data->>'barcode', invoice_date DESC, created_at DESC
            """, nativeQuery = true)
    List<TrendyolCargoInvoice> findLatestByStoreIdAndBarcodes(
            @Param("storeId") UUID storeId,
            @Param("barcodes") List<String> barcodes);

    /**
     * Get the LATEST shipping cost "share" for each barcode.
     * Uses DISTINCT ON to get the most recent cargo invoice for each barcode.
     * The shipping share is calculated as: cargo_amount × (item_price / order_gross_amount)
     *
     * Returns: BarcodeShippingShareProjection with barcode and shippingShare
     *
     * Used for FALLBACK shipping estimation when order doesn't have a cargo invoice.
     */
    @Query(value = """
            SELECT DISTINCT ON (item->>'barcode')
                item->>'barcode' as barcode,
                ROUND((ci.amount * (CAST(item->>'price' AS numeric) / NULLIF(o.gross_amount, 0)))::numeric, 2) as shipping_share
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE ci.store_id = :storeId
            AND item->>'barcode' IS NOT NULL
            AND ci.amount > 0
            AND o.gross_amount > 0
            ORDER BY item->>'barcode', ci.invoice_date DESC, ci.created_at DESC
            """, nativeQuery = true)
    List<BarcodeShippingShareProjection> findLatestShippingShareByBarcode(@Param("storeId") UUID storeId);

    /**
     * Get the LATEST shipping cost "share" for specific barcodes.
     * More efficient version that filters by barcode list.
     */
    @Query(value = """
            SELECT DISTINCT ON (item->>'barcode')
                item->>'barcode' as barcode,
                ROUND((ci.amount * (CAST(item->>'price' AS numeric) / NULLIF(o.gross_amount, 0)))::numeric, 2) as shipping_share
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE ci.store_id = :storeId
            AND item->>'barcode' IN (:barcodes)
            AND ci.amount > 0
            AND o.gross_amount > 0
            ORDER BY item->>'barcode', ci.invoice_date DESC, ci.created_at DESC
            """, nativeQuery = true)
    List<BarcodeShippingShareProjection> findLatestShippingShareByBarcodes(
            @Param("storeId") UUID storeId,
            @Param("barcodes") List<String> barcodes);

    /**
     * Get cargo invoice details for a specific product (barcode) within date range.
     * Joins with trendyol_orders to match order items by barcode.
     * Used for "Detay" panel in KARGO Ürünler tab.
     *
     * Returns cargo invoices sorted by invoice date descending, limited to 50 most recent.
     * @deprecated Use paginated version {@link #findByStoreIdAndBarcodeAndDateRangePaginated} instead
     */
    @Deprecated
    @Query(value = """
            SELECT ci.*
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(o.order_items) AS item
                WHERE item->>'barcode' = :barcode
            )
            ORDER BY ci.invoice_date DESC, ci.created_at DESC
            LIMIT 50
            """, nativeQuery = true)
    List<TrendyolCargoInvoice> findByStoreIdAndBarcodeAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get cargo invoice details for a specific product (barcode) within date range WITH PAGINATION.
     * Joins with trendyol_orders to match order items by barcode.
     * Used for "Detay" panel in KARGO Ürünler tab with lazy loading.
     *
     * @param storeId Store UUID
     * @param barcode Product barcode
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of cargo invoices
     */
    @Query(value = """
            SELECT ci.*
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(o.order_items) AS item
                WHERE item->>'barcode' = :barcode
            )
            ORDER BY ci.invoice_date DESC, ci.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(o.order_items) AS item
                WHERE item->>'barcode' = :barcode
            )
            """,
            nativeQuery = true)
    Page<TrendyolCargoInvoice> findByStoreIdAndBarcodeAndDateRangePaginated(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Get aggregate summary (totals) for a specific product (barcode) within date range.
     * Returns total amount, vat, desi, shipment count WITHOUT pagination.
     * Used for summary display while cargo list is paginated.
     *
     * @return [totalAmount, totalVatAmount, totalDesi, shipmentCount, orderCount]
     */
    @Query(value = """
            SELECT
                COALESCE(SUM(ci.amount), 0) as totalAmount,
                COALESCE(SUM(ci.vat_amount), 0) as totalVatAmount,
                COALESCE(SUM(ci.desi), 0) as totalDesi,
                COUNT(DISTINCT ci.id) as shipmentCount,
                COUNT(DISTINCT ci.order_number) as orderCount
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(o.order_items) AS item
                WHERE item->>'barcode' = :barcode
            )
            """, nativeQuery = true)
    CargoSummaryProjection getCargoSummaryByStoreIdAndBarcodeAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count distinct orders for a specific barcode within date range.
     * Used for order count in cargo breakdown panel.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT ci.order_number)
            FROM trendyol_cargo_invoices ci
            JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
            WHERE ci.store_id = :storeId
            AND ci.invoice_date BETWEEN :startDate AND :endDate
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(o.order_items) AS item
                WHERE item->>'barcode' = :barcode
            )
            """, nativeQuery = true)
    int countDistinctOrdersByStoreIdAndBarcodeAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
