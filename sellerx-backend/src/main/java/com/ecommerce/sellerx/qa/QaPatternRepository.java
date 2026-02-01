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
public interface QaPatternRepository extends JpaRepository<QaPattern, UUID> {

    // Find pattern by hash
    Optional<QaPattern> findByStoreIdAndPatternHash(UUID storeId, String patternHash);

    // Find patterns by store with pagination
    Page<QaPattern> findByStoreIdOrderByConfidenceScoreDesc(UUID storeId, Pageable pageable);

    // Find patterns by seniority level
    Page<QaPattern> findByStoreIdAndSeniorityLevelOrderByConfidenceScoreDesc(
            UUID storeId, String seniorityLevel, Pageable pageable);

    // Find auto-submit eligible patterns
    List<QaPattern> findByStoreIdAndIsAutoSubmitEligibleTrueOrderByConfidenceScoreDesc(UUID storeId);

    // Count by seniority level
    long countByStoreIdAndSeniorityLevel(UUID storeId, String seniorityLevel);

    // Count auto-submit eligible
    long countByStoreIdAndIsAutoSubmitEligibleTrue(UUID storeId);

    // Count total patterns
    long countByStoreId(UUID storeId);

    // Seniority stats query
    @Query("SELECT p.seniorityLevel AS groupName, COUNT(p) AS groupCount FROM QaPattern p " +
           "WHERE p.store.id = :storeId " +
           "GROUP BY p.seniorityLevel")
    List<GroupCountProjection> countByStoreIdGroupBySeniority(@Param("storeId") UUID storeId);

    // Find patterns that need seniority update (for batch processing)
    @Query("SELECT p FROM QaPattern p WHERE p.store.id = :storeId " +
           "AND p.approvalCount >= :minApprovals " +
           "AND p.seniorityLevel NOT IN ('SENIOR', 'EXPERT')")
    List<QaPattern> findPatternsForPromotionCheck(
            @Param("storeId") UUID storeId,
            @Param("minApprovals") int minApprovals);

    // Find patterns by product
    List<QaPattern> findByStoreIdAndProductIdOrderByConfidenceScoreDesc(UUID storeId, String productId);

    // Find patterns by category
    List<QaPattern> findByStoreIdAndCategoryOrderByConfidenceScoreDesc(UUID storeId, String category);

    // ========== Self-Learning AI Dashboard Methods ==========

    // Find all patterns by store (for pattern matching)
    List<QaPattern> findByStoreId(UUID storeId);

    // Find patterns by store, ordered by seniority level and confidence
    List<QaPattern> findByStoreIdOrderBySeniorityLevelDescConfidenceScoreDesc(UUID storeId);

    // Find auto-submit eligible patterns by store
    @Query("SELECT p FROM QaPattern p WHERE p.store.id = :storeId AND p.isAutoSubmitEligible = true ORDER BY p.confidenceScore DESC")
    List<QaPattern> findAutoSubmitEligibleByStoreId(@Param("storeId") UUID storeId);

    // Find patterns by store and seniority level (without pagination)
    List<QaPattern> findByStoreIdAndSeniorityLevel(UUID storeId, String seniorityLevel);

    // Find patterns eligible for auto-submit promotion (SENIOR with enough approvals)
    @Query("SELECT p FROM QaPattern p WHERE p.seniorityLevel = 'SENIOR' " +
           "AND p.isAutoSubmitEligible = false AND p.approvalCount >= 5 " +
           "AND p.rejectionCount = 0")
    List<QaPattern> findPatternsEligibleForAutoSubmitPromotion();

    // Get seniority statistics for a store
    @Query("SELECT new com.ecommerce.sellerx.qa.SeniorityStatsDto(" +
           "COUNT(p), " +
           "SUM(CASE WHEN p.seniorityLevel = 'JUNIOR' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN p.seniorityLevel = 'LEARNING' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN p.seniorityLevel = 'SENIOR' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN p.seniorityLevel = 'EXPERT' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN p.isAutoSubmitEligible = true THEN 1L ELSE 0L END)) " +
           "FROM QaPattern p WHERE p.store.id = :storeId")
    SeniorityStatsDto getSeniorityStats(@Param("storeId") UUID storeId);
}
