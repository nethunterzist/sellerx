package com.ecommerce.sellerx.orders;

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
}
