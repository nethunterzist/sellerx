package com.ecommerce.sellerx.crosssell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CrossSellRuleRepository extends JpaRepository<CrossSellRule, UUID> {

    List<CrossSellRule> findByStoreIdOrderByPriorityDesc(UUID storeId);

    @Query("SELECT r FROM CrossSellRule r WHERE r.store.id = :storeId AND r.active = true ORDER BY r.priority DESC")
    List<CrossSellRule> findActiveRulesByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT r FROM CrossSellRule r WHERE r.store.id = :storeId AND r.triggerType = :triggerType AND r.active = true ORDER BY r.priority DESC")
    List<CrossSellRule> findActiveRulesByStoreIdAndTriggerType(
            @Param("storeId") UUID storeId,
            @Param("triggerType") TriggerType triggerType);

    long countByStoreId(UUID storeId);
}
