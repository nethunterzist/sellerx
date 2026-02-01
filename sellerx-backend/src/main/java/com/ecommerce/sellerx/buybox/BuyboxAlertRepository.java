package com.ecommerce.sellerx.buybox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Buybox alert repository'si.
 */
@Repository
public interface BuyboxAlertRepository extends JpaRepository<BuyboxAlert, UUID> {

    /**
     * Mağazanın okunmamış alert'lerini getirir.
     */
    List<BuyboxAlert> findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(UUID storeId);

    /**
     * Mağazanın okunmamış alert sayısını döner.
     */
    int countByStoreIdAndIsReadFalse(UUID storeId);

    /**
     * Mağazanın tüm alert'lerini sayfalı getirir.
     */
    List<BuyboxAlert> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    /**
     * Mağazanın tüm okunmamış alert'lerini okundu olarak işaretler.
     */
    @Modifying
    @Query("UPDATE BuyboxAlert ba SET ba.isRead = true, ba.readAt = :now WHERE ba.store.id = :storeId AND ba.isRead = false")
    void markAllAsReadByStoreId(@Param("storeId") UUID storeId, @Param("now") LocalDateTime now);

    /**
     * Belirli bir takip edilen ürünün alert'lerini getirir.
     */
    List<BuyboxAlert> findByTrackedProductIdOrderByCreatedAtDesc(UUID trackedProductId);

    /**
     * Belirli bir takip edilen ürünün son alert'ini getirir.
     */
    @Query("SELECT ba FROM BuyboxAlert ba WHERE ba.trackedProduct.id = :trackedProductId ORDER BY ba.createdAt DESC LIMIT 1")
    BuyboxAlert findLatestByTrackedProductId(@Param("trackedProductId") UUID trackedProductId);
}
