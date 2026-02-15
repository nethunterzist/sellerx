package com.ecommerce.sellerx.crosssell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface CrossSellAnalyticsRepository extends JpaRepository<CrossSellAnalytics, UUID> {

    long countByStoreId(UUID storeId);

    long countByRuleId(UUID ruleId);

    @Query("SELECT COUNT(a) FROM CrossSellAnalytics a WHERE a.store.id = :storeId AND a.createdAt >= :since")
    long countByStoreIdSince(@Param("storeId") UUID storeId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM CrossSellAnalytics a WHERE a.store.id = :storeId AND a.wasIncludedInAnswer = true AND a.createdAt >= :since")
    long countIncludedByStoreIdSince(@Param("storeId") UUID storeId, @Param("since") LocalDateTime since);
}
