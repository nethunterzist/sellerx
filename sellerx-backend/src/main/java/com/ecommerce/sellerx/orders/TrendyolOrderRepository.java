package com.ecommerce.sellerx.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // Find returned orders
    @Query("SELECT o FROM TrendyolOrder o WHERE o.store.id = :storeId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Returned')")
    List<TrendyolOrder> findReturnedOrdersByStoreAndDateRange(@Param("storeId") UUID storeId,
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
     * İade edilen sipariş sayısı.
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
     * Updates orders with real shipping costs from cargo invoices.
     * Joins trendyol_orders with trendyol_cargo_invoices by order_number.
     * Only updates orders where is_shipping_estimated = true.
     *
     * @param storeId The store ID to update orders for
     * @return Number of orders updated
     */
    @Modifying
    @Query(value = """
        UPDATE trendyol_orders o
        SET estimated_shipping_cost = c.amount,
            is_shipping_estimated = false
        FROM trendyol_cargo_invoices c
        WHERE o.ty_order_number = c.order_number
          AND o.store_id = :storeId
          AND o.is_shipping_estimated = true
        """, nativeQuery = true)
    int updateOrdersWithCargoInvoices(@Param("storeId") UUID storeId);
}
