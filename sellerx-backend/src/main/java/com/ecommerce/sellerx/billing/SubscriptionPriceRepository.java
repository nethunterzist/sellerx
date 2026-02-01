package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPriceRepository extends JpaRepository<SubscriptionPrice, UUID> {

    List<SubscriptionPrice> findByPlanIdAndIsActiveTrue(UUID planId);

    Optional<SubscriptionPrice> findByPlanIdAndBillingCycleAndIsActiveTrue(UUID planId, BillingCycle billingCycle);

    @Query("SELECT sp FROM SubscriptionPrice sp WHERE sp.plan.code = :planCode AND sp.isActive = true ORDER BY sp.billingCycle")
    List<SubscriptionPrice> findByPlanCode(@Param("planCode") String planCode);

    @Query("SELECT sp FROM SubscriptionPrice sp WHERE sp.plan.code = :planCode AND sp.billingCycle = :billingCycle AND sp.isActive = true")
    Optional<SubscriptionPrice> findByPlanCodeAndBillingCycle(@Param("planCode") String planCode, @Param("billingCycle") BillingCycle billingCycle);

    @Query("SELECT sp FROM SubscriptionPrice sp JOIN FETCH sp.plan WHERE sp.isActive = true AND sp.plan.isActive = true ORDER BY sp.plan.sortOrder, sp.billingCycle")
    List<SubscriptionPrice> findAllActiveWithPlan();
}
