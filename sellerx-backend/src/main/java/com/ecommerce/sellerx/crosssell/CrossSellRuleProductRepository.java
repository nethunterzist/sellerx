package com.ecommerce.sellerx.crosssell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CrossSellRuleProductRepository extends JpaRepository<CrossSellRuleProduct, UUID> {

    List<CrossSellRuleProduct> findByRuleIdOrderByDisplayOrderAsc(UUID ruleId);

    void deleteByRuleId(UUID ruleId);
}
