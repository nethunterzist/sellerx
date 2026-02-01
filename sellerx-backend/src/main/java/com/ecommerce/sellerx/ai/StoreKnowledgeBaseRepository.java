package com.ecommerce.sellerx.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreKnowledgeBaseRepository extends JpaRepository<StoreKnowledgeBase, UUID> {

    List<StoreKnowledgeBase> findByStoreIdAndIsActiveTrueOrderByPriorityDesc(UUID storeId);

    List<StoreKnowledgeBase> findByStoreIdOrderByPriorityDesc(UUID storeId);

    List<StoreKnowledgeBase> findByStoreIdAndCategoryAndIsActiveTrueOrderByPriorityDesc(UUID storeId, String category);

    @Query("SELECT k FROM StoreKnowledgeBase k WHERE k.store.id = :storeId AND k.isActive = true " +
           "AND (LOWER(k.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(k.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<StoreKnowledgeBase> searchByStoreIdAndTerm(@Param("storeId") UUID storeId, @Param("searchTerm") String searchTerm);

    long countByStoreId(UUID storeId);

    long countByStoreIdAndCategory(UUID storeId, String category);
}
