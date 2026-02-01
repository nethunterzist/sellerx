package com.ecommerce.sellerx.qa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeSuggestionRepository extends JpaRepository<KnowledgeSuggestion, UUID> {

    // Find all suggestions for a store with pagination
    Page<KnowledgeSuggestion> findByStoreIdOrderByPriorityDescCreatedAtDesc(UUID storeId, Pageable pageable);

    // Find suggestions by status
    Page<KnowledgeSuggestion> findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(
            UUID storeId, String status, Pageable pageable);

    // Find pending suggestions for a store
    List<KnowledgeSuggestion> findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(UUID storeId, String status);

    // Find all suggestions for a store (without pagination)
    List<KnowledgeSuggestion> findByStoreIdOrderByPriorityDescCreatedAtDesc(UUID storeId);

    // Count pending suggestions
    long countByStoreIdAndStatus(UUID storeId, String status);

    // Count by priority
    @Query("SELECT s.priority AS groupName, COUNT(s) AS groupCount FROM KnowledgeSuggestion s " +
           "WHERE s.store.id = :storeId AND s.status = 'PENDING' " +
           "GROUP BY s.priority")
    List<GroupCountProjection> countByStoreIdGroupByPriority(@Param("storeId") UUID storeId);

    // Find suggestions with high question count
    @Query("SELECT s FROM KnowledgeSuggestion s " +
           "WHERE s.store.id = :storeId AND s.status = 'PENDING' AND s.questionCount >= :minCount " +
           "ORDER BY s.questionCount DESC")
    List<KnowledgeSuggestion> findHighPrioritySuggestions(
            @Param("storeId") UUID storeId,
            @Param("minCount") int minCount);
}
