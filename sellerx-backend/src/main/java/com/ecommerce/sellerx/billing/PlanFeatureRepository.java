package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanFeatureRepository extends JpaRepository<PlanFeature, UUID> {

    List<PlanFeature> findByPlanId(UUID planId);

    List<PlanFeature> findByPlanIdAndIsEnabledTrue(UUID planId);

    Optional<PlanFeature> findByPlanIdAndFeatureCode(UUID planId, String featureCode);

    @Query("SELECT pf FROM PlanFeature pf WHERE pf.plan.code = :planCode AND pf.featureCode = :featureCode")
    Optional<PlanFeature> findByPlanCodeAndFeatureCode(@Param("planCode") String planCode, @Param("featureCode") String featureCode);

    @Query("SELECT pf FROM PlanFeature pf WHERE pf.plan.code = :planCode AND pf.isEnabled = true")
    List<PlanFeature> findEnabledFeaturesByPlanCode(@Param("planCode") String planCode);

    @Query("SELECT pf.featureCode FROM PlanFeature pf WHERE pf.plan.id = :planId AND pf.isEnabled = true")
    List<String> findEnabledFeatureCodesByPlanId(@Param("planId") UUID planId);

    @Query("SELECT DISTINCT pf.featureCode FROM PlanFeature pf")
    List<String> findAllFeatureCodes();

    boolean existsByPlanIdAndFeatureCodeAndIsEnabledTrue(UUID planId, String featureCode);
}
