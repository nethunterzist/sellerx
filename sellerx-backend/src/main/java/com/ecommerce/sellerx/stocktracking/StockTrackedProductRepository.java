package com.ecommerce.sellerx.stocktracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTrackedProductRepository extends JpaRepository<StockTrackedProduct, UUID> {

    /**
     * Find all active tracked products for a store
     */
    List<StockTrackedProduct> findByStoreIdAndIsActiveTrueOrderByCreatedAtDesc(UUID storeId);

    /**
     * Find all tracked products for a store (including inactive)
     */
    List<StockTrackedProduct> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    /**
     * Count active tracked products for a store (for limit check)
     */
    int countByStoreIdAndIsActiveTrue(UUID storeId);

    /**
     * Check if product is already being tracked
     */
    boolean existsByStoreIdAndTrendyolProductId(UUID storeId, Long trendyolProductId);

    /**
     * Find by store and Trendyol product ID
     */
    Optional<StockTrackedProduct> findByStoreIdAndTrendyolProductId(UUID storeId, Long trendyolProductId);

    /**
     * Find all products that need to be checked (for scheduled job)
     */
    @Query(value = """
        SELECT * FROM stock_tracked_products p
        WHERE p.is_active = true
        AND (p.last_checked_at IS NULL
             OR p.last_checked_at < CURRENT_TIMESTAMP - (p.check_interval_hours * INTERVAL '1 hour'))
        ORDER BY p.last_checked_at ASC NULLS FIRST
        """, nativeQuery = true)
    List<StockTrackedProduct> findProductsNeedingCheck();

    /**
     * Find all active tracked products (for scheduled job)
     */
    @Query("SELECT p FROM StockTrackedProduct p WHERE p.isActive = true")
    List<StockTrackedProduct> findAllActiveProducts();

    /**
     * Find product with store loaded (to avoid lazy loading issues)
     */
    @Query("SELECT p FROM StockTrackedProduct p JOIN FETCH p.store WHERE p.id = :id")
    Optional<StockTrackedProduct> findByIdWithStore(@Param("id") UUID id);
}
