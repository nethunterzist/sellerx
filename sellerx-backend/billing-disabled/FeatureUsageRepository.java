package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, UUID> {

    @Query("SELECT fu FROM FeatureUsage fu WHERE fu.user.id = :userId AND fu.featureCode = :featureCode AND fu.periodStart <= :now AND fu.periodEnd >= :now")
    Optional<FeatureUsage> findCurrentUsage(@Param("userId") Long userId, @Param("featureCode") String featureCode, @Param("now") LocalDateTime now);

    @Query("SELECT fu FROM FeatureUsage fu WHERE fu.user.id = :userId AND fu.periodStart <= :now AND fu.periodEnd >= :now")
    List<FeatureUsage> findAllCurrentUsageByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    Optional<FeatureUsage> findByUserIdAndFeatureCodeAndPeriodStart(Long userId, String featureCode, LocalDateTime periodStart);

    @Query("SELECT fu FROM FeatureUsage fu WHERE fu.user.id = :userId AND fu.featureCode = :featureCode ORDER BY fu.periodStart DESC")
    List<FeatureUsage> findUsageHistory(@Param("userId") Long userId, @Param("featureCode") String featureCode);

    @Modifying
    @Query("UPDATE FeatureUsage fu SET fu.usageCount = fu.usageCount + 1, fu.updatedAt = CURRENT_TIMESTAMP WHERE fu.id = :id")
    void incrementUsage(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE FeatureUsage fu SET fu.usageCount = fu.usageCount + :amount, fu.updatedAt = CURRENT_TIMESTAMP WHERE fu.id = :id")
    void incrementUsageByAmount(@Param("id") UUID id, @Param("amount") int amount);

    @Query("SELECT SUM(fu.usageCount) FROM FeatureUsage fu WHERE fu.user.id = :userId AND fu.featureCode = :featureCode")
    Long sumTotalUsage(@Param("userId") Long userId, @Param("featureCode") String featureCode);

    @Query("SELECT fu.featureCode, fu.usageCount FROM FeatureUsage fu WHERE fu.user.id = :userId AND fu.periodStart <= :now AND fu.periodEnd >= :now")
    List<Object[]> findCurrentUsageSummary(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
