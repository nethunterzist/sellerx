package com.ecommerce.sellerx.buybox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Buybox takip edilen ürünler repository'si.
 */
@Repository
public interface BuyboxTrackedProductRepository extends JpaRepository<BuyboxTrackedProduct, UUID> {

    /**
     * Mağazanın aktif takip edilen ürünlerini getirir.
     */
    List<BuyboxTrackedProduct> findByStoreIdAndIsActiveTrue(UUID storeId);

    /**
     * Mağazanın aktif takip edilen ürün sayısını döner.
     */
    int countByStoreIdAndIsActiveTrue(UUID storeId);

    /**
     * Belirli bir mağaza ve ürün kombinasyonunu getirir.
     */
    Optional<BuyboxTrackedProduct> findByStoreIdAndProductId(UUID storeId, UUID productId);

    /**
     * Belirli bir mağaza ve ürün kombinasyonunun var olup olmadığını kontrol eder.
     */
    boolean existsByStoreIdAndProductId(UUID storeId, UUID productId);

    /**
     * Tüm aktif takip edilen ürünleri getirir (scheduled job için).
     */
    @Query("SELECT btp FROM BuyboxTrackedProduct btp WHERE btp.isActive = true")
    List<BuyboxTrackedProduct> findAllActiveForScheduledCheck();

    /**
     * Mağazanın tüm takip edilen ürünlerini (aktif/pasif) getirir.
     */
    List<BuyboxTrackedProduct> findByStoreId(UUID storeId);
}
