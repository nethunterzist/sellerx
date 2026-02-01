package com.ecommerce.sellerx.qa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolQuestionRepository extends JpaRepository<TrendyolQuestion, UUID> {

    // Find all questions for a store with pagination
    Page<TrendyolQuestion> findByStoreIdOrderByQuestionDateDesc(UUID storeId, Pageable pageable);

    // Find questions by status
    Page<TrendyolQuestion> findByStoreIdAndStatusOrderByQuestionDateDesc(UUID storeId, String status, Pageable pageable);

    // Find question by Trendyol question ID
    Optional<TrendyolQuestion> findByStoreIdAndQuestionId(UUID storeId, String questionId);

    // Find questions by product ID
    List<TrendyolQuestion> findByStoreIdAndProductId(UUID storeId, String productId);

    // Find questions by product ID and status (for AI context building)
    List<TrendyolQuestion> findByStoreIdAndProductIdAndStatus(UUID storeId, String productId, String status);

    // Check if question exists
    boolean existsByStoreIdAndQuestionId(UUID storeId, String questionId);

    // Count questions by status
    long countByStoreIdAndStatus(UUID storeId, String status);

    // Count total questions for a store
    long countByStoreId(UUID storeId);

    // Find questions created after a specific date (for pattern analysis)
    List<TrendyolQuestion> findByStoreIdAndCreatedAtAfter(UUID storeId, java.time.LocalDateTime createdAfter);

    // Search questions
    @Query("SELECT q FROM TrendyolQuestion q WHERE q.store.id = :storeId " +
           "AND (LOWER(q.customerQuestion) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(q.productTitle) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(q.barcode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TrendyolQuestion> findByStoreIdAndSearch(@Param("storeId") UUID storeId,
                                                  @Param("search") String search,
                                                  Pageable pageable);
}
