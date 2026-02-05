package com.ecommerce.sellerx.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
