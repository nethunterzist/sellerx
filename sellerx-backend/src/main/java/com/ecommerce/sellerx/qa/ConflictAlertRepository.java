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
public interface ConflictAlertRepository extends JpaRepository<ConflictAlert, UUID> {

    // Find all alerts for a store with pagination
    Page<ConflictAlert> findByStoreIdOrderBySeverityDescCreatedAtDesc(UUID storeId, Pageable pageable);

    // Find alerts by status
    Page<ConflictAlert> findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
            UUID storeId, String status, Pageable pageable);

    // Find active alerts for a store
    List<ConflictAlert> findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(UUID storeId, String status);

    // Find alerts by question
    List<ConflictAlert> findByQuestionIdAndStatus(UUID questionId, String status);

    // Find active alert for a question
    Optional<ConflictAlert> findFirstByQuestionIdAndStatus(UUID questionId, String status);

    // Count active alerts
    long countByStoreIdAndStatus(UUID storeId, String status);

    // Count by severity
    @Query("SELECT a.severity AS groupName, COUNT(a) AS groupCount FROM ConflictAlert a " +
           "WHERE a.store.id = :storeId AND a.status = 'ACTIVE' " +
           "GROUP BY a.severity")
    List<GroupCountProjection> countByStoreIdGroupBySeverity(@Param("storeId") UUID storeId);

    // Count by conflict type
    @Query("SELECT a.conflictType AS groupName, COUNT(a) AS groupCount FROM ConflictAlert a " +
           "WHERE a.store.id = :storeId AND a.status = 'ACTIVE' " +
           "GROUP BY a.conflictType")
    List<GroupCountProjection> countByStoreIdGroupByType(@Param("storeId") UUID storeId);

    // Find critical/high alerts
    @Query("SELECT a FROM ConflictAlert a " +
           "WHERE a.store.id = :storeId AND a.status = 'ACTIVE' " +
           "AND a.severity IN ('CRITICAL', 'HIGH') " +
           "ORDER BY CASE a.severity WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 END, a.createdAt DESC")
    List<ConflictAlert> findCriticalAlerts(@Param("storeId") UUID storeId);

    // Check if question has active alert
    boolean existsByQuestionIdAndStatus(UUID questionId, String status);

    // ========== Self-Learning AI Dashboard Methods ==========

    // Find all alerts by store, ordered by creation date (without status filter)
    List<ConflictAlert> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    // Get conflict statistics for a store
    @Query("SELECT new com.ecommerce.sellerx.qa.ConflictStatsDto(" +
           "COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.severity = 'CRITICAL' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.severity = 'HIGH' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.severity = 'MEDIUM' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.severity = 'LOW' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.conflictType = 'LEGAL_RISK' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.conflictType = 'HEALTH_SAFETY' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.conflictType = 'KNOWLEDGE_CONFLICT' AND c.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN c.conflictType = 'BRAND_INCONSISTENCY' AND c.status = 'ACTIVE' THEN 1 END)) " +
           "FROM ConflictAlert c WHERE c.store.id = :storeId")
    ConflictStatsDto getConflictStats(@Param("storeId") UUID storeId);

    // Check if question has critical alerts
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConflictAlert c " +
           "WHERE c.question.id = :questionId AND c.severity = 'CRITICAL' AND c.status = 'ACTIVE'")
    boolean hasCriticalAlertsForQuestion(@Param("questionId") UUID questionId);
}
