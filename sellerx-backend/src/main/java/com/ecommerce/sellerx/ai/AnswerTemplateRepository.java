package com.ecommerce.sellerx.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnswerTemplateRepository extends JpaRepository<AnswerTemplate, UUID> {

    List<AnswerTemplate> findByStoreIdAndIsActiveTrueOrderByUsageCountDesc(UUID storeId);

    List<AnswerTemplate> findByStoreIdOrderByUsageCountDesc(UUID storeId);

    List<AnswerTemplate> findByStoreIdAndCategoryAndIsActiveTrueOrderByUsageCountDesc(UUID storeId, String category);

    long countByStoreId(UUID storeId);
}
