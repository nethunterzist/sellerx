package com.ecommerce.sellerx.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    Optional<PurchaseOrderItem> findByPurchaseOrderIdAndId(Long purchaseOrderId, Long itemId);

    void deleteByPurchaseOrderIdAndId(Long purchaseOrderId, Long itemId);

    // Find items by product ID and store ID (for cost history reporting)
    @Query("SELECT poi FROM PurchaseOrderItem poi " +
           "JOIN poi.purchaseOrder po " +
           "WHERE poi.product.id = :productId AND po.store.id = :storeId " +
           "ORDER BY po.poDate DESC")
    List<PurchaseOrderItem> findByProductIdAndStoreId(@Param("productId") UUID productId, @Param("storeId") UUID storeId);
}
