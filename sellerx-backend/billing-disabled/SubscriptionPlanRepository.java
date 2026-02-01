package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByCode(String code);

    List<SubscriptionPlan> findByIsActiveTrueOrderBySortOrderAsc();

    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true AND sp.code != 'FREE' ORDER BY sp.sortOrder")
    List<SubscriptionPlan> findPaidPlans();

    Optional<SubscriptionPlan> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);
}
