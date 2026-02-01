package com.ecommerce.sellerx.purchasing;

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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByStoreIdOrderByPoDateDesc(UUID storeId);

    List<PurchaseOrder> findByStoreIdAndStatusOrderByPoDateDesc(UUID storeId, PurchaseOrderStatus status);

    Optional<PurchaseOrder> findByStoreIdAndId(UUID storeId, Long id);

    boolean existsByStoreIdAndPoNumber(UUID storeId, String poNumber);

    // Stats queries
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.store.id = :storeId AND po.status = :status")
    Long countByStoreIdAndStatus(@Param("storeId") UUID storeId, @Param("status") PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalCost), 0) FROM PurchaseOrder po WHERE po.store.id = :storeId AND po.status = :status")
    BigDecimal sumTotalCostByStoreIdAndStatus(@Param("storeId") UUID storeId, @Param("status") PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalUnits), 0) FROM PurchaseOrder po WHERE po.store.id = :storeId AND po.status = :status")
    Long sumTotalUnitsByStoreIdAndStatus(@Param("storeId") UUID storeId, @Param("status") PurchaseOrderStatus status);

    // For generating PO number
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.store.id = :storeId")
    Long countByStoreId(@Param("storeId") UUID storeId);

    // Search by PO number or supplier name
    @Query("SELECT po FROM PurchaseOrder po WHERE po.store.id = :storeId " +
           "AND (LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(po.supplierName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY po.poDate DESC")
    List<PurchaseOrder> searchByTerm(@Param("storeId") UUID storeId, @Param("search") String search);

    // Filter by supplier ID
    List<PurchaseOrder> findByStoreIdAndSupplierIdOrderByPoDateDesc(UUID storeId, Long supplierId);

    // Filter by supplier ID and status
    @Query("SELECT po FROM PurchaseOrder po WHERE po.store.id = :storeId " +
           "AND po.supplier.id = :supplierId AND po.status = :status " +
           "ORDER BY po.poDate DESC")
    List<PurchaseOrder> findByStoreIdAndSupplierIdAndStatus(
            @Param("storeId") UUID storeId,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status);

    // Find child POs (split from parent)
    List<PurchaseOrder> findByStoreIdAndParentPoIdOrderByPoDateDesc(UUID storeId, Long parentPoId);

    // Date range query for reports
    @Query("SELECT po FROM PurchaseOrder po WHERE po.store.id = :storeId " +
           "AND po.poDate BETWEEN :startDate AND :endDate AND po.status = :status " +
           "ORDER BY po.poDate DESC")
    List<PurchaseOrder> findByStoreIdAndPoDateBetweenAndStatus(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") PurchaseOrderStatus status);
}
