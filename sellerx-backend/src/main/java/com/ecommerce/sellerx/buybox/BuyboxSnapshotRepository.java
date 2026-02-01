package com.ecommerce.sellerx.buybox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Buybox snapshot repository'si.
 */
@Repository
public interface BuyboxSnapshotRepository extends JpaRepository<BuyboxSnapshot, UUID> {

    /**
     * Takip edilen ürünün en son snapshot'ını getirir.
     */
    Optional<BuyboxSnapshot> findTopByTrackedProductIdOrderByCheckedAtDesc(UUID trackedProductId);

    /**
     * Takip edilen ürünün snapshot geçmişini sayfalı getirir.
     */
    List<BuyboxSnapshot> findByTrackedProductIdOrderByCheckedAtDesc(UUID trackedProductId, Pageable pageable);

    /**
     * Mağazanın tüm ürünlerinin en son snapshot'larını getirir.
     */
    @Query("SELECT bs FROM BuyboxSnapshot bs WHERE bs.trackedProduct.store.id = :storeId " +
           "AND bs.checkedAt = (SELECT MAX(bs2.checkedAt) FROM BuyboxSnapshot bs2 WHERE bs2.trackedProduct = bs.trackedProduct)")
    List<BuyboxSnapshot> findLatestSnapshotsByStoreId(@Param("storeId") UUID storeId);

    /**
     * Takip edilen ürünün tüm snapshot'larını getirir.
     */
    List<BuyboxSnapshot> findByTrackedProductIdOrderByCheckedAtDesc(UUID trackedProductId);

    /**
     * Takip edilen ürünün snapshot sayısını döner.
     */
    int countByTrackedProductId(UUID trackedProductId);
}
