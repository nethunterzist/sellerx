package com.ecommerce.sellerx.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
public interface TrendyolOrderRepository extends JpaRepository<TrendyolOrder, UUID> {

    // Find orders by store
    Page<TrendyolOrder> findByStoreIdOrderByOrderDateDesc(UUID storeId, Pageable pageable);

    // Find orders by store and date range
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    Page<TrendyolOrder> findByStoreAndDateRange(@Param("storeId") UUID storeId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    // Find by store and Trendyol order number
    List<TrendyolOrder> findByStoreIdAndTyOrderNumber(UUID storeId, String tyOrderNumber);

    // Find by store and package number
    Optional<TrendyolOrder> findByStoreIdAndPackageNo(UUID storeId, Long packageNo);

    // Check if order exists by store and package number
    boolean existsByStoreIdAndPackageNo(UUID storeId, Long packageNo);

    // Batch check existing package numbers
    @Query("SELECT o.packageNo FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.packageNo IN :packageNumbers")
    List<Long> findExistingPackageNumbers(@Param("storeId") UUID storeId, @Param("packageNumbers") List<Long> packageNumbers);

    // Find orders by store and package numbers (for batch updates)
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.packageNo IN :packageNumbers")
    List<TrendyolOrder> findByStoreIdAndPackageNoIn(@Param("storeId") UUID storeId, @Param("packageNumbers") List<Long> packageNumbers);

    // Find orders without city information for a store
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.shipmentCity IS NULL")
    List<TrendyolOrder> findByStoreIdAndShipmentCityIsNull(@Param("storeId") UUID storeId);

    // Dashboard Stats Queries

    // Find orders for revenue calculation (excluding cancelled, returned etc.)
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    List<TrendyolOrder> findRevenueOrdersByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    // Count orders with items that have costs vs without costs
    @Query(value = "SELECT COUNT(DISTINCT o.id) " +
                   "FROM trendyol_orders o " +
                   "WHERE o.store_id = :storeId " +
                   "AND o.order_date BETWEEN :startDate AND :endDate " +
                   "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked') " +
                   "AND EXISTS (SELECT 1 FROM jsonb_array_elements(o.order_items) AS item " +
                   "            WHERE (item->>'cost') IS NOT NULL)",
           nativeQuery = true)
    Long countOrdersWithCosts(@Param("storeId") UUID storeId,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(DISTINCT o.id) " +
                   "FROM trendyol_orders o " +
                   "WHERE o.store_id = :storeId " +
                   "AND o.order_date BETWEEN :startDate AND :endDate " +
                   "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked') " +
                   "AND EXISTS (SELECT 1 FROM jsonb_array_elements(o.order_items) AS item " +
                   "            WHERE (item->>'cost') IS NULL)",
           nativeQuery = true)
    Long countOrdersWithoutCosts(@Param("storeId") UUID storeId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // Find orders by status
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.status = :status ORDER BY o.orderDate DESC")
    Page<TrendyolOrder> findByStoreAndStatus(@Param("storeId") UUID storeId,
                                           @Param("status") String status,
                                           Pageable pageable);

    // Get orders count by store
    long countByStoreId(UUID storeId);

    // Get orders count by store and status
    long countByStoreIdAndStatus(UUID storeId, String status);

    // Get orders count by store and multiple statuses
    long countByStoreIdAndStatusIn(UUID storeId, List<String> statuses);

    // Count orders by store and date range
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate")
    long countByStoreIdAndOrderDateBetween(@Param("storeId") UUID storeId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Count orders by store, status and date range
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.status = :status AND o.orderDate BETWEEN :startDate AND :endDate")
    long countByStoreIdAndStatusAndOrderDateBetween(@Param("storeId") UUID storeId,
                                                    @Param("status") String status,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // Count orders by store, multiple statuses and date range
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.status IN :statuses AND o.orderDate BETWEEN :startDate AND :endDate")
    long countByStoreIdAndStatusInAndOrderDateBetween(@Param("storeId") UUID storeId,
                                                      @Param("statuses") List<String> statuses,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // Calculate total revenue by date range (excluding cancelled and returned orders)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0.0) FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate AND o.status NOT IN :excludedStatuses")
    BigDecimal sumTotalPriceByStoreIdAndOrderDateBetweenAndStatusNotIn(@Param("storeId") UUID storeId,
                                                                        @Param("startDate") LocalDateTime startDate,
                                                                        @Param("endDate") LocalDateTime endDate,
                                                                        @Param("excludedStatuses") List<String> excludedStatuses);

    // Calculate total revenue (excluding cancelled and returned orders)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0.0) FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.status NOT IN :excludedStatuses")
    BigDecimal sumTotalPriceByStoreIdAndStatusNotIn(@Param("storeId") UUID storeId, @Param("excludedStatuses") List<String> excludedStatuses);

    // Find orders containing specific product from a date onwards
    @Query(value = "SELECT * FROM trendyol_orders o WHERE o.store_id = :storeId " +
           "AND o.order_date >= :fromDate " +
           "AND EXISTS (SELECT 1 FROM jsonb_array_elements(o.order_items) AS item " +
           "WHERE item->>'barcode' = :barcode) " +
           "ORDER BY o.order_date ASC", nativeQuery = true)
    List<TrendyolOrder> findOrdersWithProductFromDate(@Param("storeId") UUID storeId,
                                                      @Param("barcode") String barcode,
                                                      @Param("fromDate") LocalDateTime fromDate);

    // Settlement related queries

    // Find order by order number, package ID and store (for settlement matching)
    Optional<TrendyolOrder> findByTyOrderNumberAndPackageNoAndStore(String tyOrderNumber, Long packageNo, com.ecommerce.sellerx.stores.Store store);

    // Count orders by store
    long countByStore(com.ecommerce.sellerx.stores.Store store);

    // Count orders by store and transaction status
    long countByStoreAndTransactionStatus(com.ecommerce.sellerx.stores.Store store, String transactionStatus);

    // Find orders by store and transaction status
    List<TrendyolOrder> findByStoreAndTransactionStatus(com.ecommerce.sellerx.stores.Store store, String transactionStatus);

    // Financial reconciliation queries

    // Find orders by store and date range with specific statuses (for VAT calculation)
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN :statuses ORDER BY o.orderDate DESC")
    List<TrendyolOrder> findByStoreIdAndOrderDateBetweenAndStatusIn(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<String> statuses);

    // Find all orders by store and date range (for commission breakdown)
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<TrendyolOrder> findByStoreIdAndOrderDateBetween(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find orders that have a specific paymentOrderId in their financial transactions (JSONB query)
    @Query(value = "SELECT * FROM trendyol_orders o WHERE o.store_id = :storeId " +
           "AND EXISTS (SELECT 1 FROM jsonb_array_elements(o.financial_transactions) as ft, " +
           "jsonb_array_elements(ft->'transactions') as t " +
           "WHERE (t->>'paymentOrderId')::bigint = :paymentOrderId)", nativeQuery = true)
    List<TrendyolOrder> findByStoreIdAndFinancialPaymentOrderId(
            @Param("storeId") UUID storeId,
            @Param("paymentOrderId") Long paymentOrderId);

    /**
     * Find orders that have a specific commissionInvoiceSerialNumber in their financial transactions (JSONB query).
     * Used to show order-level breakdown of a commission invoice.
     */
    @Query(value = "SELECT DISTINCT o.* FROM trendyol_orders o, " +
           "jsonb_array_elements(o.financial_transactions) as ft, " +
           "jsonb_array_elements(ft->'transactions') as t " +
           "WHERE o.store_id = :storeId " +
           "AND t->>'commissionInvoiceSerialNumber' = :invoiceSerialNumber " +
           "ORDER BY o.order_date DESC", nativeQuery = true)
    List<TrendyolOrder> findByStoreIdAndCommissionInvoiceSerialNumber(
            @Param("storeId") UUID storeId,
            @Param("invoiceSerialNumber") String invoiceSerialNumber);

    /**
     * Aggregate commission data by barcode for a date range.
     * Calculates commission per product based on order_items (estimatedCommissionRate and vatBaseAmount).
     * Returns: [barcode, productName, totalQuantity, totalCommission, totalVatBaseAmount, orderCount]
     * Used for "Ürünler" tab in KOMISYON category view.
     */
    @Query(value = """
            SELECT
                item->>'barcode' as barcode,
                MAX(item->>'productName') as productName,
                COALESCE(SUM((item->>'quantity')::integer), 0) as totalQuantity,
                COALESCE(SUM(
                    CASE
                        WHEN (item->>'estimatedCommissionRate')::numeric > 0
                        THEN (item->>'vatBaseAmount')::numeric * (item->>'estimatedCommissionRate')::numeric / 100
                        ELSE 0
                    END
                ), 0) as totalCommission,
                COALESCE(SUM(
                    CASE
                        WHEN (item->>'estimatedCommissionRate')::numeric > 0
                        THEN (item->>'vatBaseAmount')::numeric * (item->>'estimatedCommissionRate')::numeric / 100 * 0.20
                        ELSE 0
                    END
                ), 0) as totalVatAmount,
                COUNT(DISTINCT o.id) as orderCount
            FROM trendyol_orders o
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE o.store_id = :storeId
            AND o.order_date BETWEEN :startDate AND :endDate
            AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            AND item->>'barcode' IS NOT NULL
            GROUP BY item->>'barcode'
            ORDER BY totalCommission DESC
            """, nativeQuery = true)
    List<CommissionByBarcodeProjection> aggregateCommissionByBarcode(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate commission data by barcode from financial_transactions JSONB.
     * Uses actual settlement data instead of estimated commission from order_items.
     *
     * IMPORTANT: This calculates NET commission correctly:
     * - Satış (Sale): ADD commission (goes to Trendyol)
     * - İndirim (Discount): SUBTRACT commission (returned to seller)
     * - Kupon (Coupon): SUBTRACT commission (returned to seller)
     * - İade (Return): EXCLUDED (separate invoice in Trendyol)
     *
     * Returns: [barcode, productName, orderCount, transactionCount, netCommission,
     *           saleCommission, discountCommission, couponCommission, netVatAmount]
     */
    @Query(value = """
            SELECT
                ft->>'barcode' as barcode,
                NULL as productName,
                COUNT(DISTINCT o.id) as orderCount,
                COUNT(tx) as transactionCount,

                -- Net komisyon: Satış - İndirim - Kupon
                COALESCE(SUM(
                    CASE
                        WHEN tx->>'transactionType' = 'Satış' THEN (tx->>'commissionAmount')::numeric
                        WHEN tx->>'transactionType' IN ('İndirim', 'Kupon') THEN -(tx->>'commissionAmount')::numeric
                        ELSE 0
                    END
                ), 0) as netCommission,

                -- Satış komisyonu toplamı
                COALESCE(SUM(CASE WHEN tx->>'transactionType' = 'Satış'
                             THEN (tx->>'commissionAmount')::numeric ELSE 0 END), 0) as saleCommission,

                -- İndirim komisyonu toplamı (pozitif olarak saklanır)
                COALESCE(SUM(CASE WHEN tx->>'transactionType' = 'İndirim'
                             THEN (tx->>'commissionAmount')::numeric ELSE 0 END), 0) as discountCommission,

                -- Kupon komisyonu toplamı (pozitif olarak saklanır)
                COALESCE(SUM(CASE WHEN tx->>'transactionType' = 'Kupon'
                             THEN (tx->>'commissionAmount')::numeric ELSE 0 END), 0) as couponCommission,

                -- Net KDV (%20)
                COALESCE(SUM(
                    CASE
                        WHEN tx->>'transactionType' = 'Satış' THEN (tx->>'commissionAmount')::numeric * 0.20
                        WHEN tx->>'transactionType' IN ('İndirim', 'Kupon') THEN -(tx->>'commissionAmount')::numeric * 0.20
                        ELSE 0
                    END
                ), 0) as netVatAmount
            FROM trendyol_orders o
            CROSS JOIN LATERAL jsonb_array_elements(o.financial_transactions) AS ft
            CROSS JOIN LATERAL jsonb_array_elements(ft->'transactions') AS tx
            WHERE o.store_id = :storeId
            AND o.order_date BETWEEN :startDate AND :endDate
            AND tx->>'transactionType' IN ('Satış', 'İndirim', 'Kupon')
            GROUP BY ft->>'barcode'
            ORDER BY netCommission DESC
            """, nativeQuery = true)
    List<SettlementCommissionByBarcodeProjection> aggregateCommissionByBarcodeFromSettlements(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // City statistics queries

    // Get city statistics (order count, total revenue) by store and date range
    @Query(value = "SELECT shipment_city as city, shipment_city_code as cityCode, " +
           "COUNT(*) as orderCount, COALESCE(SUM(total_price), 0) as totalRevenue, " +
           "COALESCE(SUM((SELECT COALESCE(SUM((item->>'quantity')::integer), 0) FROM jsonb_array_elements(order_items) item)), 0) as totalQuantity " +
           "FROM trendyol_orders " +
           "WHERE store_id = :storeId " +
           "AND order_date BETWEEN :startDate AND :endDate " +
           "AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked') " +
           "AND shipment_city IS NOT NULL " +
           "GROUP BY shipment_city, shipment_city_code " +
           "ORDER BY orderCount DESC", nativeQuery = true)
    List<CityStatsProjection> findCityStatsByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // Count orders without city information
    @Query(value = "SELECT COUNT(*) FROM trendyol_orders " +
           "WHERE store_id = :storeId " +
           "AND order_date BETWEEN :startDate AND :endDate " +
           "AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked') " +
           "AND shipment_city IS NULL", nativeQuery = true)
    Long countOrdersWithoutCity(@Param("storeId") UUID storeId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // Get city statistics filtered by product barcode
    @Query(value = "SELECT shipment_city as city, shipment_city_code as cityCode, " +
           "COUNT(*) as orderCount, COALESCE(SUM(total_price), 0) as totalRevenue, " +
           "COALESCE(SUM((SELECT COALESCE(SUM((item->>'quantity')::integer), 0) FROM jsonb_array_elements(order_items) item WHERE item->>'barcode' = :barcode)), 0) as totalQuantity " +
           "FROM trendyol_orders " +
           "WHERE store_id = :storeId " +
           "AND order_date BETWEEN :startDate AND :endDate " +
           "AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked') " +
           "AND shipment_city IS NOT NULL " +
           "AND EXISTS (SELECT 1 FROM jsonb_array_elements(order_items) item WHERE item->>'barcode' = :barcode) " +
           "GROUP BY shipment_city, shipment_city_code " +
           "ORDER BY orderCount DESC", nativeQuery = true)
    List<CityStatsProjection> findCityStatsByStoreAndDateRangeAndProduct(@Param("storeId") UUID storeId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate,
                                                               @Param("barcode") String barcode);

    // Commission recalculation queries

    /**
     * Find orders that need commission recalculation:
     * - isCommissionEstimated = true (not yet confirmed by Financial API)
     * - estimatedCommission = 0 or null
     * These are orders where commission couldn't be calculated during initial sync
     * because products didn't have commission rates yet.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.isCommissionEstimated = true " +
           "AND (o.estimatedCommission IS NULL OR o.estimatedCommission = 0)")
    List<TrendyolOrder> findOrdersNeedingCommissionRecalculation(@Param("storeId") UUID storeId);

    /**
     * Find orders by store, data source, and order date after a specific date.
     * Used for enriching historical orders with operational data from Orders API.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.dataSource = :dataSource " +
           "AND o.orderDate >= :orderDate ORDER BY o.orderDate ASC")
    List<TrendyolOrder> findByStoreIdAndDataSourceAndOrderDateAfter(
        @Param("storeId") UUID storeId,
        @Param("dataSource") String dataSource,
        @Param("orderDate") LocalDateTime orderDate);

    // ================= HYBRID SYNC - Gap Analysis Queries =================

    /**
     * Find orders with estimated commission that need reconciliation.
     * These orders came from Orders API and haven't received real commission from Settlement API yet.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.isCommissionEstimated = true")
    List<TrendyolOrder> findByStoreIdAndIsCommissionEstimatedTrue(@Param("storeId") UUID storeId);

    /**
     * Find orders with estimated commission in a date range.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.isCommissionEstimated = true ORDER BY o.orderDate ASC")
    List<TrendyolOrder> findByStoreIdAndOrderDateBetweenAndIsCommissionEstimatedTrue(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find the latest order date for a specific data source.
     * Used to determine where Settlement data ends (gap start).
     */
    @Query("SELECT CAST(MAX(o.orderDate) AS LocalDate) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId AND o.dataSource = :dataSource")
    java.time.LocalDate findLatestOrderDateByStoreIdAndDataSource(
            @Param("storeId") UUID storeId,
            @Param("dataSource") String dataSource);

    /**
     * Find the earliest order date for a specific data source.
     * Used for historical sync analysis.
     */
    @Query("SELECT CAST(MIN(o.orderDate) AS LocalDate) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId AND o.dataSource = :dataSource")
    java.time.LocalDate findEarliestOrderDateByStoreIdAndDataSource(
            @Param("storeId") UUID storeId,
            @Param("dataSource") String dataSource);

    /**
     * Count orders with estimated commission (not yet reconciled).
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.isCommissionEstimated = true")
    int countByStoreIdAndIsCommissionEstimatedTrue(@Param("storeId") UUID storeId);

    /**
     * Count orders by data source.
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.dataSource = :dataSource")
    int countByStoreIdAndDataSource(@Param("storeId") UUID storeId, @Param("dataSource") String dataSource);

    /**
     * Find orders by order number for reconciliation matching.
     * Used when Settlement API provides data to match with existing Orders API records.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.tyOrderNumber = :orderNumber")
    List<TrendyolOrder> findByStoreIdAndTyOrderNumberForReconciliation(
            @Param("storeId") UUID storeId,
            @Param("orderNumber") String orderNumber);

    // ============== İndirim ve Finansal Metrik Sorguları ==============

    /**
     * Toplam satıcı indirimi (total_discount - total_ty_discount - coupon_discount).
     */
    @Query("SELECT COALESCE(SUM(o.totalDiscount - o.totalTyDiscount - COALESCE(o.couponDiscount, 0)), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    BigDecimal sumSellerDiscount(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam platform indirimi (total_ty_discount alanı - Trendyol'un verdiği indirim).
     */
    @Query("SELECT COALESCE(SUM(o.totalTyDiscount), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    BigDecimal sumPlatformDiscount(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam brüt tutar (gross_amount).
     */
    @Query("SELECT COALESCE(SUM(o.grossAmount), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    BigDecimal sumGrossAmount(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam komisyon (estimated_commission).
     */
    @Query("SELECT COALESCE(SUM(o.estimatedCommission), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    BigDecimal sumEstimatedCommission(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * İade edilen sipariş sayısı (sadece status bazlı).
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status = 'Returned'")
    Integer countReturnedOrders(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam sipariş sayısı (hesaplamalar için).
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked', 'Returned')")
    Integer countAllOrdersForPeriod(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam kupon indirimi (coupon_discount - Trendyol kupon indirimi).
     */
    @Query("SELECT COALESCE(SUM(o.couponDiscount), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status NOT IN ('Cancelled', 'UnSupplied')")
    BigDecimal sumCouponDiscount(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Toplam erken ödeme kesintisi (early_payment_fee).
     */
    @Query("SELECT COALESCE(SUM(o.earlyPaymentFee), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status NOT IN ('Cancelled', 'UnSupplied')")
    BigDecimal sumEarlyPaymentFee(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============== Stoppage Queries (siparişlerden hesaplanan %1) ==============

    /**
     * Sum stoppage from orders (calculated 1% withholding tax) for a date range.
     * Used by invoice summary to show stoppage total from order-based calculation.
     */
    @Query("SELECT COALESCE(SUM(o.stoppage), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status NOT IN ('Cancelled')")
    BigDecimal sumStoppageByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders with non-zero stoppage for a date range.
     * Used by invoice summary to show stoppage count.
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status NOT IN ('Cancelled') " +
           "AND o.stoppage IS NOT NULL AND o.stoppage > 0")
    int countOrdersWithStoppage(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============== Platform-wide Admin Queries ==============

    /**
     * Count all orders across all stores in a date range (for admin dashboard).
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    long countAllOrdersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count all orders across all stores by status (for admin dashboard).
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.status = :status")
    long countAllOrdersByStatus(@Param("status") String status);

    /**
     * Find all orders across all stores ordered by orderDate descending (for admin recent orders).
     */
    @Query("SELECT o FROM TrendyolOrder o ORDER BY o.orderDate DESC")
    Page<TrendyolOrder> findAllByOrderByOrderDateDesc(Pageable pageable);

    /**
     * Find top products by order count across all stores (for admin top products).
     * Aggregates order_items JSONB to rank products by how many orders contain them.
     * Returns: [barcode, title, storeName, orderCount, totalRevenue]
     */
    @Query(value = """
            SELECT
                item->>'barcode' as barcode,
                MAX(item->>'productName') as title,
                MAX(s.store_name) as storeName,
                COUNT(DISTINCT o.id) as orderCount,
                COALESCE(SUM((item->>'amount')::numeric), 0) as totalRevenue
            FROM trendyol_orders o
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            JOIN stores s ON o.store_id = s.id
            WHERE o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            AND item->>'barcode' IS NOT NULL
            GROUP BY item->>'barcode'
            ORDER BY orderCount DESC
            LIMIT 50
            """, nativeQuery = true)
    List<TopProductProjection> findTopProductsByOrderCount();

    // ============== Tarihi Sipariş Sorguları ==============

    /**
     * Find the earliest order date for a store (any data source).
     * Used by historical cargo invoice sync to determine how far back to sync.
     *
     * @param storeId The store ID to find earliest order for
     * @return Optional containing the earliest order date, or empty if no orders exist
     */
    @Query("SELECT CAST(MIN(o.orderDate) AS LocalDate) FROM TrendyolOrder o WHERE o.store.id = :storeId")
    Optional<java.time.LocalDate> findEarliestOrderDateByStoreId(@Param("storeId") UUID storeId);

    // ============== Kargo Faturası ile Güncelleme ==============

    /**
     * Updates orders with real OUTBOUND shipping costs from cargo invoices.
     * Filters by shipment_package_type = 'Gönderi Kargo Bedeli' to exclude return cargo.
     * Uses SUM to handle orders with multiple cargo invoice entries.
     * Only updates orders where is_shipping_estimated = true.
     *
     * @param storeId The store ID to update orders for
     * @return Number of orders updated
     */
    @Modifying
    @Query(value = """
        UPDATE trendyol_orders o
        SET estimated_shipping_cost = sub.total_cost,
            is_shipping_estimated = false
        FROM (
            SELECT c.order_number, c.store_id, SUM(c.amount) as total_cost
            FROM trendyol_cargo_invoices c
            WHERE c.store_id = :storeId
              AND c.shipment_package_type = 'Gönderi Kargo Bedeli'
            GROUP BY c.order_number, c.store_id
        ) sub
        WHERE o.ty_order_number = sub.order_number
          AND o.store_id = sub.store_id
          AND o.is_shipping_estimated = true
        """, nativeQuery = true)
    int updateOrdersWithCargoInvoices(@Param("storeId") UUID storeId);

    /**
     * Updates orders with real RETURN shipping costs from cargo invoices.
     * Filters by shipment_package_type = 'İade Kargo Bedeli'.
     * Uses SUM to handle orders with multiple return cargo invoice entries.
     * Only updates orders where return_shipping_cost is still 0.
     *
     * @param storeId The store ID to update orders for
     * @return Number of orders updated
     */
    @Modifying
    @Query(value = """
        UPDATE trendyol_orders o
        SET return_shipping_cost = sub.total_cost
        FROM (
            SELECT c.order_number, c.store_id, SUM(c.amount) as total_cost
            FROM trendyol_cargo_invoices c
            WHERE c.store_id = :storeId
              AND c.shipment_package_type = 'İade Kargo Bedeli'
            GROUP BY c.order_number, c.store_id
        ) sub
        WHERE o.ty_order_number = sub.order_number
          AND o.store_id = sub.store_id
          AND (o.return_shipping_cost IS NULL OR o.return_shipping_cost = 0)
        """, nativeQuery = true)
    int updateOrdersWithReturnCargoInvoices(@Param("storeId") UUID storeId);

    // ============== Customer Analytics Queries ==============

    /**
     * Customer analytics summary: total customers, repeat customers, revenue breakdown.
     * Uses customer_id (stable numeric ID) instead of customer_email (masked by Trendyol).
     * Repeat customer = customer who placed 2+ separate orders (order_count >= 2).
     * avgItemsPerCustomer = average total items purchased per customer (based on quantity from order_items JSONB)
     */
    @Query(value = """
            WITH customer_stats AS (
                SELECT customer_id,
                       COUNT(*) as order_count,
                       COALESCE(SUM(
                           (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                            FROM jsonb_array_elements(order_items) AS item)
                       ), 0) as item_count,
                       SUM(total_price) as total_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            )
            SELECT
                COUNT(*) as totalCustomers,
                COUNT(*) FILTER (WHERE order_count >= 2) as repeatCustomers,
                COALESCE(SUM(total_spend), 0) as totalRevenue,
                COALESCE(SUM(total_spend) FILTER (WHERE order_count >= 2), 0) as repeatRevenue,
                COALESCE(AVG(order_count), 0) as avgOrdersPerCustomer,
                COALESCE(AVG(item_count), 0) as avgItemsPerCustomer,
                COALESCE(CAST(SUM(item_count) AS DOUBLE PRECISION) / NULLIF(SUM(order_count), 0), 0) as avgItemsPerOrder
            FROM customer_stats
            """, nativeQuery = true)
    com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.SummaryProjection
    getCustomerAnalyticsSummary(@Param("storeId") UUID storeId);

    /**
     * Customer segmentation: by order count (1 / 2-3 / 4-6 / 7+ orders placed).
     * Uses number of orders instead of item count for accurate loyalty segmentation.
     * Segments: 1 (new customer), 2-3 (returning), 4-6 (regular), 7+ (loyal)
     */
    @Query(value = """
            WITH customer_stats AS (
                SELECT customer_id,
                       COUNT(*) as order_count,
                       SUM(total_price) as total_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            )
            SELECT
                CASE
                    WHEN order_count = 1 THEN '1'
                    WHEN order_count BETWEEN 2 AND 3 THEN '2-3'
                    WHEN order_count BETWEEN 4 AND 6 THEN '4-6'
                    ELSE '7+'
                END as segment,
                COUNT(*) as customerCount,
                COALESCE(SUM(total_spend), 0) as totalRevenue
            FROM customer_stats
            GROUP BY 1
            ORDER BY MIN(order_count)
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.SegmentProjection>
    getCustomerSegmentation(@Param("storeId") UUID storeId);

    /**
     * City-based repeat customer analysis (top 20 cities).
     * Repeat customer = customer who placed 2+ separate orders (order_count >= 2).
     */
    @Query(value = """
            WITH customer_city AS (
                SELECT customer_id,
                       shipment_city as city,
                       COUNT(*) as order_count,
                       SUM(total_price) as total_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND shipment_city IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id, shipment_city
            )
            SELECT
                city,
                COUNT(*) as totalCustomers,
                COUNT(*) FILTER (WHERE order_count >= 2) as repeatCustomers,
                COALESCE(SUM(total_spend), 0) as totalRevenue
            FROM customer_city
            GROUP BY city
            ORDER BY totalCustomers DESC
            LIMIT 20
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CityRepeatProjection>
    getCityRepeatAnalysis(@Param("storeId") UUID storeId);

    /**
     * Monthly new vs repeat customer trend (last 12 months).
     */
    @Query(value = """
            WITH monthly_orders AS (
                SELECT
                    TO_CHAR(order_date, 'YYYY-MM') as month,
                    customer_id,
                    MIN(order_date) OVER (PARTITION BY customer_id) as first_order_date,
                    SUM(total_price) as monthly_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND order_date >= NOW() - INTERVAL '12 months'
                GROUP BY TO_CHAR(order_date, 'YYYY-MM'), customer_id, order_date
            ),
            monthly_classified AS (
                SELECT
                    month,
                    customer_id,
                    CASE
                        WHEN TO_CHAR(first_order_date, 'YYYY-MM') = month THEN 'new'
                        ELSE 'repeat'
                    END as customer_type,
                    monthly_spend
                FROM monthly_orders
            )
            SELECT
                month,
                COUNT(DISTINCT customer_id) FILTER (WHERE customer_type = 'new') as newCustomers,
                COUNT(DISTINCT customer_id) FILTER (WHERE customer_type = 'repeat') as repeatCustomers,
                COALESCE(SUM(monthly_spend) FILTER (WHERE customer_type = 'new'), 0) as newRevenue,
                COALESCE(SUM(monthly_spend) FILTER (WHERE customer_type = 'repeat'), 0) as repeatRevenue
            FROM monthly_classified
            GROUP BY month
            ORDER BY month ASC
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.MonthlyTrendProjection>
    getMonthlyNewVsRepeatTrend(@Param("storeId") UUID storeId);

    /**
     * Product-level repeat buyer analysis using JSONB order_items.
     * Calculates how many unique buyers each product has, how many are repeat buyers.
     * Repeat buyer = customer who purchased the same product in 2+ separate orders.
     */
    @Query(value = """
            WITH product_buyers AS (
                SELECT
                    item->>'barcode' as barcode,
                    MAX(item->>'productName') as product_name,
                    o.customer_id,
                    COUNT(*) as purchase_count,
                    SUM((item->>'quantity')::integer) as total_qty
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' IS NOT NULL
                GROUP BY item->>'barcode', o.customer_id
            )
            SELECT
                pb.barcode,
                MAX(pb.product_name) as productName,
                COUNT(*) as totalBuyers,
                COUNT(*) FILTER (WHERE pb.purchase_count >= 2) as repeatBuyers,
                COALESCE(SUM(pb.total_qty), 0) as totalQuantitySold,
                MAX(p.image) as image,
                MAX(p.product_url) as productUrl
            FROM product_buyers pb
            LEFT JOIN trendyol_products p ON pb.barcode = p.barcode AND p.store_id = :storeId
            GROUP BY pb.barcode
            HAVING COUNT(*) >= 3
            ORDER BY repeatBuyers DESC, totalBuyers DESC
            LIMIT 50
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.ProductRepeatProjection>
    getProductRepeatAnalysis(@Param("storeId") UUID storeId);

    /**
     * Cross-sell analysis: products frequently bought together by the same customer.
     * Returns top product pairs with co-occurrence count and confidence.
     */
    @Query(value = """
            WITH customer_products AS (
                SELECT DISTINCT
                    o.customer_id,
                    item->>'barcode' as barcode,
                    MAX(item->>'productName') as product_name
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' IS NOT NULL
                GROUP BY o.customer_id, item->>'barcode'
            ),
            product_pairs AS (
                SELECT
                    a.barcode as source_barcode,
                    a.product_name as source_product_name,
                    b.barcode as target_barcode,
                    b.product_name as target_product_name,
                    COUNT(DISTINCT a.customer_id) as co_occurrence_count
                FROM customer_products a
                JOIN customer_products b ON a.customer_id = b.customer_id AND a.barcode < b.barcode
                GROUP BY a.barcode, a.product_name, b.barcode, b.product_name
                HAVING COUNT(DISTINCT a.customer_id) >= 2
            )
            SELECT
                pp.source_barcode as sourceBarcode,
                pp.source_product_name as sourceProductName,
                pp.target_barcode as targetBarcode,
                pp.target_product_name as targetProductName,
                pp.co_occurrence_count as coOccurrenceCount,
                sp.image as sourceImage,
                sp.product_url as sourceProductUrl,
                tp.image as targetImage,
                tp.product_url as targetProductUrl
            FROM product_pairs pp
            LEFT JOIN trendyol_products sp ON pp.source_barcode = sp.barcode AND sp.store_id = :storeId
            LEFT JOIN trendyol_products tp ON pp.target_barcode = tp.barcode AND tp.store_id = :storeId
            ORDER BY pp.co_occurrence_count DESC
            LIMIT 30
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CrossSellProjection>
    getCrossSellAnalysis(@Param("storeId") UUID storeId);

    /**
     * Paginated customer list with aggregated stats.
     * itemCount = total quantity of items purchased (sum of quantity from order_items JSONB)
     */
    @Query(value = """
            SELECT
                customer_id as customerId,
                CONCAT(MAX(customer_first_name), ' ', MAX(customer_last_name)) as displayName,
                MAX(shipment_city) as city,
                COUNT(*) as orderCount,
                COALESCE(SUM(
                    (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                     FROM jsonb_array_elements(order_items) AS item)
                ), 0) as itemCount,
                COALESCE(SUM(total_price), 0) as totalSpend,
                MIN(order_date) as firstOrderDate,
                MAX(order_date) as lastOrderDate
            FROM trendyol_orders
            WHERE store_id = :storeId
            AND customer_id IS NOT NULL
            AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            GROUP BY customer_id
            ORDER BY totalSpend DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CustomerSummaryProjection>
    findCustomerSummaries(@Param("storeId") UUID storeId,
                          @Param("limit") int limit,
                          @Param("offset") int offset);

    /**
     * Count distinct customers for pagination.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT customer_id)
            FROM trendyol_orders
            WHERE store_id = :storeId
            AND customer_id IS NOT NULL
            AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            """, nativeQuery = true)
    long countDistinctCustomers(@Param("storeId") UUID storeId);

    /**
     * Paginated customer list with search filter.
     * itemCount = total quantity of items purchased (sum of quantity from order_items JSONB)
     */
    @Query(value = """
            SELECT
                customer_id as customerId,
                CONCAT(MAX(customer_first_name), ' ', MAX(customer_last_name)) as displayName,
                MAX(shipment_city) as city,
                COUNT(*) as orderCount,
                COALESCE(SUM(
                    (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                     FROM jsonb_array_elements(order_items) AS item)
                ), 0) as itemCount,
                COALESCE(SUM(total_price), 0) as totalSpend,
                MIN(order_date) as firstOrderDate,
                MAX(order_date) as lastOrderDate
            FROM trendyol_orders
            WHERE store_id = :storeId
            AND customer_id IS NOT NULL
            AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            GROUP BY customer_id
            HAVING LOWER(CONCAT(MAX(customer_first_name), ' ', MAX(customer_last_name))) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(MAX(shipment_city)) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY totalSpend DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CustomerSummaryProjection>
    findCustomerSummariesWithSearch(@Param("storeId") UUID storeId,
                                    @Param("search") String search,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    /**
     * Count distinct customers with search filter.
     */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT customer_id
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
                HAVING LOWER(CONCAT(MAX(customer_first_name), ' ', MAX(customer_last_name))) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(MAX(shipment_city)) LIKE LOWER(CONCAT('%', :search, '%'))
            ) as filtered_customers
            """, nativeQuery = true)
    long countDistinctCustomersWithSearch(@Param("storeId") UUID storeId, @Param("search") String search);

    /**
     * Customer data backfill coverage: total orders vs orders with customer_id.
     */
    @Query(value = """
            SELECT
                COUNT(*) as totalOrders,
                COUNT(customer_id) as ordersWithCustomerData
            FROM trendyol_orders
            WHERE store_id = :storeId
            AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            """, nativeQuery = true)
    com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.BackfillCoverageProjection
    getCustomerDataCoverage(@Param("storeId") UUID storeId);

    /**
     * Average repeat interval in days for customers who ordered more than once.
     */
    @Query(value = """
            WITH customer_orders AS (
                SELECT customer_id, order_date,
                       LAG(order_date) OVER (PARTITION BY customer_id ORDER BY order_date) as prev_order_date
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            )
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (order_date - prev_order_date)) / 86400), 0)
            FROM customer_orders
            WHERE prev_order_date IS NOT NULL
            """, nativeQuery = true)
    Double getAvgRepeatIntervalDays(@Param("storeId") UUID storeId);

    /**
     * Average days between repurchase per product (for products with repeat buyers).
     */
    @Query(value = """
            WITH product_customer_orders AS (
                SELECT
                    item->>'barcode' as barcode,
                    o.customer_id,
                    o.order_date,
                    LAG(o.order_date) OVER (PARTITION BY item->>'barcode', o.customer_id ORDER BY o.order_date) as prev_order_date
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' IS NOT NULL
            )
            SELECT barcode,
                   COALESCE(AVG(EXTRACT(EPOCH FROM (order_date - prev_order_date)) / 86400), 0) as avgDays
            FROM product_customer_orders
            WHERE prev_order_date IS NOT NULL
            GROUP BY barcode
            """, nativeQuery = true)
    List<Object[]> getProductAvgRepeatIntervalDays(@Param("storeId") UUID storeId);

    /**
     * Find distinct order numbers that have no customer data (customer_id IS NULL).
     */
    @Query(value = """
            SELECT DISTINCT ty_order_number
            FROM trendyol_orders
            WHERE store_id = :storeId
            AND customer_id IS NULL
            AND ty_order_number IS NOT NULL
            AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked', 'Returned', 'Cancelled', 'UnSupplied')
            ORDER BY ty_order_number
            """, nativeQuery = true)
    List<String> findOrderNumbersWithoutCustomerData(@Param("storeId") UUID storeId);

    // ============== Advanced Customer Analytics Queries ==============

    /**
     * Customer Lifecycle Stages: categorize customers by their lifecycle stage.
     * Uses item_count (total items purchased) instead of order_count for more accurate segmentation.
     * Stages: new (30d), active (30d + 3+ items), loyal (10+ items + 60d active),
     * at_risk (60-120d), dormant (120-180d), lost (180+d)
     */
    @Query(value = """
            WITH customer_stats AS (
                SELECT
                    customer_id,
                    COUNT(*) as order_count,
                    COALESCE(SUM(
                        (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                         FROM jsonb_array_elements(order_items) AS item)
                    ), 0) as item_count,
                    SUM(total_price) as total_spend,
                    MIN(order_date) as first_order_date,
                    MAX(order_date) as last_order_date
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            ),
            lifecycle_classified AS (
                SELECT
                    customer_id,
                    total_spend,
                    CASE
                        WHEN first_order_date > NOW() - INTERVAL '30 days' THEN 'new'
                        WHEN last_order_date > NOW() - INTERVAL '30 days' AND item_count >= 3 THEN 'active'
                        WHEN item_count >= 10 AND last_order_date > NOW() - INTERVAL '60 days' THEN 'loyal'
                        WHEN last_order_date BETWEEN NOW() - INTERVAL '120 days' AND NOW() - INTERVAL '60 days' THEN 'at_risk'
                        WHEN last_order_date BETWEEN NOW() - INTERVAL '180 days' AND NOW() - INTERVAL '120 days' THEN 'dormant'
                        WHEN last_order_date < NOW() - INTERVAL '180 days' THEN 'lost'
                        ELSE 'other'
                    END as lifecycle_stage
                FROM customer_stats
            )
            SELECT
                lifecycle_stage as lifecycleStage,
                COUNT(*) as customerCount,
                COALESCE(SUM(total_spend), 0) as totalRevenue
            FROM lifecycle_classified
            GROUP BY lifecycle_stage
            ORDER BY
                CASE lifecycle_stage
                    WHEN 'new' THEN 1
                    WHEN 'active' THEN 2
                    WHEN 'loyal' THEN 3
                    WHEN 'at_risk' THEN 4
                    WHEN 'dormant' THEN 5
                    WHEN 'lost' THEN 6
                    ELSE 7
                END
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.LifecycleStageProjection>
    getCustomerLifecycleStages(@Param("storeId") UUID storeId);

    /**
     * Cohort Analysis: Monthly cohort retention data.
     * Groups customers by their first purchase month and tracks which months they returned.
     */
    @Query(value = """
            WITH customer_first_order AS (
                SELECT
                    customer_id,
                    DATE_TRUNC('month', MIN(order_date)) as cohort_month
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            ),
            customer_order_months AS (
                SELECT DISTINCT
                    o.customer_id,
                    DATE_TRUNC('month', o.order_date) as order_month
                FROM trendyol_orders o
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            )
            SELECT
                TO_CHAR(f.cohort_month, 'YYYY-MM') as cohortMonth,
                TO_CHAR(m.order_month, 'YYYY-MM') as orderMonth,
                COUNT(DISTINCT f.customer_id) as activeCustomers
            FROM customer_first_order f
            JOIN customer_order_months m ON f.customer_id = m.customer_id
            WHERE f.cohort_month >= NOW() - INTERVAL '12 months'
            GROUP BY f.cohort_month, m.order_month
            ORDER BY f.cohort_month, m.order_month
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CohortProjection>
    getCohortAnalysis(@Param("storeId") UUID storeId);

    /**
     * Purchase Frequency Distribution: how many customers placed 1, 2-3, 4-6, 7-10, 11+ orders.
     * Uses order count to measure customer return behavior and loyalty.
     */
    @Query(value = """
            WITH customer_orders AS (
                SELECT
                    customer_id,
                    COUNT(*) as order_count,
                    SUM(total_price) as total_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            )
            SELECT
                CASE
                    WHEN order_count = 1 THEN '1'
                    WHEN order_count BETWEEN 2 AND 3 THEN '2-3'
                    WHEN order_count BETWEEN 4 AND 6 THEN '4-6'
                    WHEN order_count BETWEEN 7 AND 10 THEN '7-10'
                    ELSE '11+'
                END as frequencyBucket,
                COUNT(*) as customerCount,
                COALESCE(SUM(total_spend), 0) as totalRevenue,
                COALESCE(SUM(order_count), 0) as totalOrders
            FROM customer_orders
            GROUP BY 1
            ORDER BY MIN(order_count)
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.FrequencyDistributionProjection>
    getPurchaseFrequencyDistribution(@Param("storeId") UUID storeId);

    /**
     * CLV Summary Statistics: average CLV, top 10% CLV, top 10% revenue share.
     * CLV = Total Spend (simplified model)
     */
    @Query(value = """
            WITH customer_clv AS (
                SELECT
                    customer_id,
                    SUM(total_price) as clv
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            ),
            clv_stats AS (
                SELECT
                    AVG(clv) as avg_clv,
                    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY clv) as median_clv,
                    PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY clv) as p90_clv,
                    SUM(clv) as total_revenue,
                    COUNT(*) as total_customers
                FROM customer_clv
            ),
            top_10_revenue AS (
                SELECT SUM(clv) as top_10_revenue
                FROM (
                    SELECT clv FROM customer_clv ORDER BY clv DESC LIMIT (SELECT GREATEST(total_customers / 10, 1) FROM clv_stats)
                ) t
            )
            SELECT
                s.avg_clv as avgClv,
                s.median_clv as medianClv,
                s.p90_clv as top10PercentClv,
                CASE WHEN s.total_revenue > 0
                     THEN (t.top_10_revenue / s.total_revenue) * 100
                     ELSE 0
                END as top10PercentRevenueShare
            FROM clv_stats s, top_10_revenue t
            """, nativeQuery = true)
    com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.ClvSummaryProjection
    getClvSummary(@Param("storeId") UUID storeId);

    // ============== Customer Detail Queries ==============

    /**
     * Get per-customer average repeat interval in days.
     * Returns avgDays for each customer who has more than 1 order.
     */
    @Query(value = """
            WITH customer_order_gaps AS (
                SELECT
                    customer_id,
                    order_date,
                    LAG(order_date) OVER (PARTITION BY customer_id ORDER BY order_date) as prev_order_date
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            )
            SELECT
                customer_id as customerId,
                AVG(EXTRACT(EPOCH FROM (order_date - prev_order_date)) / 86400) as avgDays
            FROM customer_order_gaps
            WHERE prev_order_date IS NOT NULL
            GROUP BY customer_id
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.CustomerRepeatIntervalProjection>
    getPerCustomerRepeatIntervalDays(@Param("storeId") UUID storeId);

    /**
     * Find all orders for a specific customer.
     * Used for customer detail panel.
     */
    List<TrendyolOrder> findByStoreIdAndCustomerIdOrderByOrderDateDesc(UUID storeId, Long customerId);

    /**
     * Find orders for a specific customer with pagination.
     * Used for lazy loading in customer detail panel.
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.customerId = :customerId ORDER BY o.orderDate DESC")
    List<TrendyolOrder> findByStoreIdAndCustomerIdPaginated(
            @Param("storeId") UUID storeId,
            @Param("customerId") Long customerId,
            Pageable pageable);

    /**
     * Count total orders for a specific customer.
     * Used for pagination in customer detail panel.
     */
    @Query("SELECT COUNT(o) FROM TrendyolOrder o WHERE o.store.id = :storeId AND o.customerId = :customerId")
    long countByStoreIdAndCustomerId(@Param("storeId") UUID storeId, @Param("customerId") Long customerId);

    /**
     * Find customers who purchased a specific product by barcode.
     * Returns customer info with purchase count and total spend on that product.
     * Used for product detail panel.
     */
    @Query(value = """
            WITH product_buyers AS (
                SELECT
                    o.customer_id,
                    CONCAT(MAX(o.customer_first_name), ' ', MAX(o.customer_last_name)) as customer_name,
                    MAX(o.shipment_city) as city,
                    COUNT(*) as purchase_count,
                    COALESCE(SUM((item->>'price')::numeric), 0) as total_spend
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' = :barcode
                GROUP BY o.customer_id
            )
            SELECT
                customer_id as customerId,
                customer_name as customerName,
                city,
                purchase_count as purchaseCount,
                total_spend as totalSpend
            FROM product_buyers
            ORDER BY purchase_count DESC, total_spend DESC
            LIMIT 100
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.ProductBuyerProjection>
    findProductBuyersByBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);

    /**
     * Get product repeat stats for a single product by barcode.
     * Used for product detail panel.
     */
    @Query(value = """
            WITH product_buyers AS (
                SELECT
                    o.customer_id,
                    COUNT(*) as purchase_count,
                    SUM((item->>'quantity')::integer) as total_qty
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' = :barcode
                GROUP BY o.customer_id
            )
            SELECT
                COUNT(*) as totalBuyers,
                COUNT(*) FILTER (WHERE purchase_count >= 2) as repeatBuyers,
                COALESCE(SUM(total_qty), 0) as totalQuantitySold
            FROM product_buyers
            """, nativeQuery = true)
    Object[] getProductRepeatStatsByBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);

    /**
     * Get average days between repurchase for a specific product.
     */
    @Query(value = """
            WITH product_customer_orders AS (
                SELECT
                    o.customer_id,
                    o.order_date,
                    LAG(o.order_date) OVER (PARTITION BY o.customer_id ORDER BY o.order_date) as prev_order_date
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' = :barcode
            )
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (order_date - prev_order_date)) / 86400), 0)
            FROM product_customer_orders
            WHERE prev_order_date IS NOT NULL
            """, nativeQuery = true)
    Double getProductAvgRepeatIntervalDaysByBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);

    /**
     * Summary stats with total order count for avgOrderValue calculation.
     */
    @Query(value = """
            WITH customer_data AS (
                SELECT
                    customer_id,
                    COUNT(*) as order_count,
                    COALESCE(SUM(
                        (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                         FROM jsonb_array_elements(order_items) AS item)
                    ), 0) as item_count,
                    COALESCE(SUM(total_price), 0) as total_spend
                FROM trendyol_orders
                WHERE store_id = :storeId
                AND customer_id IS NOT NULL
                AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY customer_id
            )
            SELECT
                COUNT(*) as totalCustomers,
                COUNT(*) FILTER (WHERE order_count >= 2) as repeatCustomers,
                COALESCE(SUM(total_spend), 0) as totalRevenue,
                COALESCE(SUM(total_spend) FILTER (WHERE order_count >= 2), 0) as repeatRevenue,
                COALESCE(AVG(order_count), 0) as avgOrdersPerCustomer,
                COALESCE(AVG(item_count), 0) as avgItemsPerCustomer,
                CASE WHEN SUM(order_count) > 0
                     THEN SUM(item_count)::float / SUM(order_count)
                     ELSE 0
                END as avgItemsPerOrder,
                COALESCE(SUM(order_count), 0) as totalOrders
            FROM customer_data
            """, nativeQuery = true)
    com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.SummaryWithOrderCountProjection
    getCustomerAnalyticsSummaryWithOrderCount(@Param("storeId") UUID storeId);

    /**
     * Find customers who purchased a specific product by barcode (PAGINATED).
     * Returns customer info with purchase count and total spend on that product.
     * Used for lazy loading in product detail panel.
     */
    @Query(value = """
            WITH product_buyers AS (
                SELECT
                    o.customer_id,
                    CONCAT(MAX(o.customer_first_name), ' ', MAX(o.customer_last_name)) as customer_name,
                    MAX(o.shipment_city) as city,
                    COUNT(*) as purchase_count,
                    COALESCE(SUM((item->>'price')::numeric), 0) as total_spend
                FROM trendyol_orders o
                CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                AND item->>'barcode' = :barcode
                GROUP BY o.customer_id
            )
            SELECT
                customer_id as customerId,
                customer_name as customerName,
                city,
                purchase_count as purchaseCount,
                total_spend as totalSpend
            FROM product_buyers
            ORDER BY purchase_count DESC, total_spend DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.ProductBuyerProjection>
    findProductBuyersByBarcodePaginated(
            @Param("storeId") UUID storeId,
            @Param("barcode") String barcode,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Count total buyers for a specific product by barcode.
     * Used for pagination in product detail panel.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT o.customer_id)
            FROM trendyol_orders o
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE o.store_id = :storeId
            AND o.customer_id IS NOT NULL
            AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            AND item->>'barcode' = :barcode
            """, nativeQuery = true)
    long countProductBuyersByBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);

    // ============== Stock Valuation - Sales Velocity Queries ==============

    /**
     * Get total quantity sold per barcode in a date range.
     * Used for calculating average daily sales velocity for stock depletion estimation.
     * Returns: [barcode, totalQuantitySold]
     */
    @Query(value = """
            SELECT
                item->>'barcode' as barcode,
                COALESCE(SUM((item->>'quantity')::integer), 0) as totalQuantitySold
            FROM trendyol_orders o
            CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
            WHERE o.store_id = :storeId
            AND o.order_date >= :startDate
            AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
            AND item->>'barcode' IS NOT NULL
            GROUP BY item->>'barcode'
            """, nativeQuery = true)
    List<SalesVelocityProjection> getSalesVelocityByBarcode(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Projection interface for sales velocity query.
     */
    interface SalesVelocityProjection {
        String getBarcode();
        Long getTotalQuantitySold();
    }

    /**
     * Sum estimated shipping costs for orders in a date range.
     * Used by dashboard as fallback when deduction invoices haven't arrived yet.
     */
    @Query("SELECT COALESCE(SUM(o.estimatedShippingCost), 0) FROM TrendyolOrder o " +
           "WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')")
    BigDecimal sumEstimatedShippingCostByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Recalculate estimated shipping costs for orders that still have is_shipping_estimated=true
     * and no shipping cost set. Uses MAX(lastShippingCostPerUnit) across order items because
     * Trendyol charges shipping per PACKAGE, not per item.
     *
     * @param storeId The store ID
     * @return Number of orders updated
     */
    @Modifying
    @Query(value = """
        UPDATE trendyol_orders o
        SET estimated_shipping_cost = sub.estimated_cost
        FROM (
            SELECT o2.id,
                   MAX(COALESCE(p.last_shipping_cost_per_unit, 0)) as estimated_cost
            FROM trendyol_orders o2
            CROSS JOIN LATERAL jsonb_array_elements(o2.order_items) AS item
            LEFT JOIN trendyol_products p
              ON p.barcode = item->>'barcode' AND p.store_id = o2.store_id
            WHERE o2.store_id = :storeId
              AND o2.is_shipping_estimated = true
              AND (o2.estimated_shipping_cost IS NULL OR o2.estimated_shipping_cost = 0)
            GROUP BY o2.id
        ) sub
        WHERE o.id = sub.id AND sub.estimated_cost > 0
        """, nativeQuery = true)
    int recalculateEstimatedShipping(@Param("storeId") UUID storeId);

    /**
     * Find all orders for a store ordered by date (no pagination).
     */
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId ORDER BY o.orderDate DESC")
    List<TrendyolOrder> findAllByStoreIdOrderByOrderDateDesc(@Param("storeId") UUID storeId);

    /**
     * Delete all orders for a store.
     */
    @Modifying
    @Query("DELETE FROM TrendyolOrder o WHERE o.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") UUID storeId);

}
