package com.ecommerce.sellerx.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolProductRepository extends JpaRepository<TrendyolProduct, UUID> {
    
    List<TrendyolProduct> findByStoreId(UUID storeId);
    
    // Pagination support for store products
    Page<TrendyolProduct> findByStoreId(UUID storeId, Pageable pageable);
    
    // Search with pagination
    @Query("SELECT tp FROM TrendyolProduct tp WHERE tp.store.id = :storeId " +
           "AND (LOWER(tp.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(tp.barcode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(tp.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(tp.categoryName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TrendyolProduct> findByStoreIdAndSearch(@Param("storeId") UUID storeId, 
                                                @Param("search") String search, 
                                                Pageable pageable);
    
    Optional<TrendyolProduct> findByStoreIdAndProductId(UUID storeId, String productId);

    @Query("SELECT tp FROM TrendyolProduct tp WHERE tp.store.id = :storeId AND tp.id = :id")
    Optional<TrendyolProduct> findByStoreIdAndId(@Param("storeId") UUID storeId, @Param("id") UUID id);

    @Query("SELECT tp FROM TrendyolProduct tp WHERE tp.store.id = :storeId AND tp.barcode = :barcode")
    Optional<TrendyolProduct> findByStoreIdAndBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);

    // Batch query for multiple barcodes - fixes N+1 query problem
    @Query("SELECT tp FROM TrendyolProduct tp WHERE tp.store.id = :storeId AND tp.barcode IN :barcodes")
    List<TrendyolProduct> findByStoreIdAndBarcodeIn(@Param("storeId") UUID storeId, @Param("barcodes") List<String> barcodes);

    boolean existsByStoreIdAndProductId(UUID storeId, String productId);
    
    long countByStoreId(UUID storeId);

    List<TrendyolProduct> findByStoreIdAndStockDepletedTrue(UUID storeId);

    // Advanced filtering with native query (including JSONB cost filtering)
    @Query(value = """
        SELECT tp.* FROM trendyol_products tp
        WHERE tp.store_id = :storeId
        AND (:search IS NULL OR :search = ''
            OR LOWER(tp.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.barcode) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.brand) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.category_name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:minStock IS NULL OR tp.trendyol_quantity >= :minStock)
        AND (:maxStock IS NULL OR tp.trendyol_quantity <= :maxStock)
        AND (:minPrice IS NULL OR tp.sale_price >= :minPrice)
        AND (:maxPrice IS NULL OR tp.sale_price <= :maxPrice)
        AND (:minCommission IS NULL OR tp.last_commission_rate >= :minCommission)
        AND (:maxCommission IS NULL OR tp.last_commission_rate <= :maxCommission)
        AND (:minCost IS NULL OR (
            tp.cost_and_stock_info IS NOT NULL
            AND jsonb_array_length(tp.cost_and_stock_info) > 0
            AND (
                SELECT (elem->>'unitCost')::numeric
                FROM jsonb_array_elements(tp.cost_and_stock_info) elem
                ORDER BY (elem->>'stockDate')::date DESC NULLS LAST
                LIMIT 1
            ) >= :minCost
        ))
        AND (:maxCost IS NULL OR (
            tp.cost_and_stock_info IS NOT NULL
            AND jsonb_array_length(tp.cost_and_stock_info) > 0
            AND (
                SELECT (elem->>'unitCost')::numeric
                FROM jsonb_array_elements(tp.cost_and_stock_info) elem
                ORDER BY (elem->>'stockDate')::date DESC NULLS LAST
                LIMIT 1
            ) <= :maxCost
        ))
        ORDER BY tp.on_sale DESC, tp.sale_price DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM trendyol_products tp
        WHERE tp.store_id = :storeId
        AND (:search IS NULL OR :search = ''
            OR LOWER(tp.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.barcode) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.brand) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.category_name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:minStock IS NULL OR tp.trendyol_quantity >= :minStock)
        AND (:maxStock IS NULL OR tp.trendyol_quantity <= :maxStock)
        AND (:minPrice IS NULL OR tp.sale_price >= :minPrice)
        AND (:maxPrice IS NULL OR tp.sale_price <= :maxPrice)
        AND (:minCommission IS NULL OR tp.last_commission_rate >= :minCommission)
        AND (:maxCommission IS NULL OR tp.last_commission_rate <= :maxCommission)
        AND (:minCost IS NULL OR (
            tp.cost_and_stock_info IS NOT NULL
            AND jsonb_array_length(tp.cost_and_stock_info) > 0
            AND (
                SELECT (elem->>'unitCost')::numeric
                FROM jsonb_array_elements(tp.cost_and_stock_info) elem
                ORDER BY (elem->>'stockDate')::date DESC NULLS LAST
                LIMIT 1
            ) >= :minCost
        ))
        AND (:maxCost IS NULL OR (
            tp.cost_and_stock_info IS NOT NULL
            AND jsonb_array_length(tp.cost_and_stock_info) > 0
            AND (
                SELECT (elem->>'unitCost')::numeric
                FROM jsonb_array_elements(tp.cost_and_stock_info) elem
                ORDER BY (elem->>'stockDate')::date DESC NULLS LAST
                LIMIT 1
            ) <= :maxCost
        ))
        """,
        nativeQuery = true)
    Page<TrendyolProduct> findByStoreIdWithFilters(
        @Param("storeId") UUID storeId,
        @Param("search") String search,
        @Param("minStock") Integer minStock,
        @Param("maxStock") Integer maxStock,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minCommission") BigDecimal minCommission,
        @Param("maxCommission") BigDecimal maxCommission,
        @Param("minCost") BigDecimal minCost,
        @Param("maxCost") BigDecimal maxCost,
        Pageable pageable);

    // Count products with cost info (has non-empty costAndStockInfo with unitCost > 0)
    @Query(value = """
        SELECT COUNT(*) FROM trendyol_products tp
        WHERE tp.cost_and_stock_info IS NOT NULL
        AND jsonb_array_length(tp.cost_and_stock_info) > 0
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(tp.cost_and_stock_info) elem
            WHERE (elem->>'unitCost')::numeric > 0
        )
        """, nativeQuery = true)
    long countWithCostInfo();

    // Count products grouped by store (returns store_id, store_name, count)
    @Query(value = """
        SELECT s.store_name as storeName, COUNT(tp.id) as productCount
        FROM trendyol_products tp
        JOIN stores s ON tp.store_id = s.id
        GROUP BY s.id, s.store_name
        ORDER BY productCount DESC
        """, nativeQuery = true)
    List<Object[]> countProductsByStore();

    // Delete all products for a store
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM TrendyolProduct tp WHERE tp.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") UUID storeId);

    // ==================== Buybox Queries ====================

    @Query("SELECT COUNT(p) FROM TrendyolProduct p WHERE p.store.id = :storeId AND p.onSale = true")
    int countOnSaleProducts(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(p) FROM TrendyolProduct p WHERE p.store.id = :storeId AND p.buyboxOrder = 1 AND p.hasMultipleSeller = true AND p.onSale = true")
    int countBuyboxWinning(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(p) FROM TrendyolProduct p WHERE p.store.id = :storeId AND p.hasMultipleSeller = true AND p.buyboxOrder IS NOT NULL AND p.buyboxOrder > 1 AND p.onSale = true")
    int countBuyboxLosing(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(p) FROM TrendyolProduct p WHERE p.store.id = :storeId AND p.hasMultipleSeller = true AND p.onSale = true")
    int countWithCompetitors(@Param("storeId") UUID storeId);

    // Buybox filtered product list - native query for performance
    @Query(value = """
        SELECT tp.* FROM trendyol_products tp
        WHERE tp.store_id = :storeId
        AND tp.on_sale = true
        AND (:search IS NULL OR :search = ''
            OR LOWER(tp.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR :status = ''
            OR (:status = 'WINNING' AND tp.buybox_order = 1 AND tp.has_multiple_seller = true)
            OR (:status = 'LOSING' AND tp.has_multiple_seller = true AND tp.buybox_order IS NOT NULL AND tp.buybox_order > 1)
            OR (:status = 'NO_COMPETITION' AND (tp.has_multiple_seller = false OR tp.has_multiple_seller IS NULL))
            OR (:status = 'NOT_CHECKED' AND tp.buybox_order IS NULL))
        ORDER BY
            CASE WHEN :sortBy = 'buyboxOrder' AND :sortDir = 'asc' THEN tp.buybox_order END ASC,
            CASE WHEN :sortBy = 'buyboxOrder' AND :sortDir = 'desc' THEN tp.buybox_order END DESC,
            CASE WHEN :sortBy = 'salePrice' AND :sortDir = 'asc' THEN tp.sale_price END ASC,
            CASE WHEN :sortBy = 'salePrice' AND :sortDir = 'desc' THEN tp.sale_price END DESC,
            CASE WHEN :sortBy = 'priceDifference' AND :sortDir = 'asc' THEN (tp.sale_price - COALESCE(tp.buybox_price, tp.sale_price)) END ASC,
            CASE WHEN :sortBy = 'priceDifference' AND :sortDir = 'desc' THEN (tp.sale_price - COALESCE(tp.buybox_price, tp.sale_price)) END DESC,
            tp.buybox_order ASC NULLS LAST
        """,
        countQuery = """
        SELECT COUNT(*) FROM trendyol_products tp
        WHERE tp.store_id = :storeId
        AND tp.on_sale = true
        AND (:search IS NULL OR :search = ''
            OR LOWER(tp.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tp.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR :status = ''
            OR (:status = 'WINNING' AND tp.buybox_order = 1 AND tp.has_multiple_seller = true)
            OR (:status = 'LOSING' AND tp.has_multiple_seller = true AND tp.buybox_order IS NOT NULL AND tp.buybox_order > 1)
            OR (:status = 'NO_COMPETITION' AND (tp.has_multiple_seller = false OR tp.has_multiple_seller IS NULL))
            OR (:status = 'NOT_CHECKED' AND tp.buybox_order IS NULL))
        """,
        nativeQuery = true)
    Page<TrendyolProduct> findBuyboxProducts(
        @Param("storeId") UUID storeId,
        @Param("search") String search,
        @Param("status") String status,
        @Param("sortBy") String sortBy,
        @Param("sortDir") String sortDir,
        Pageable pageable);

    // Find on-sale products with barcodes for buybox API
    @Query("SELECT p FROM TrendyolProduct p WHERE p.store.id = :storeId AND p.onSale = true AND p.barcode IS NOT NULL")
    List<TrendyolProduct> findOnSaleProductsWithBarcode(@Param("storeId") UUID storeId);
}
