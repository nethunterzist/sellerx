package com.ecommerce.sellerx.alerts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AlertRule entity.
 * Provides methods for querying user-defined alert rules.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    /**
     * Find all rules for a user.
     */
    List<AlertRule> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all active rules for a user.
     */
    List<AlertRule> findByUserIdAndActiveOrderByCreatedAtDesc(Long userId, Boolean active);

    /**
     * Find all rules for a specific store.
     */
    List<AlertRule> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    /**
     * Find all active rules for a specific store or global rules (store is null).
     */
    @Query("SELECT r FROM AlertRule r WHERE r.user.id = :userId AND r.active = true " +
           "AND (r.store.id = :storeId OR r.store IS NULL) ORDER BY r.createdAt DESC")
    List<AlertRule> findActiveRulesForUserAndStore(@Param("userId") Long userId, @Param("storeId") UUID storeId);

    /**
     * Find all active rules by alert type for a user.
     */
    @Query("SELECT r FROM AlertRule r WHERE r.user.id = :userId AND r.alertType = :alertType " +
           "AND r.active = true AND (r.store.id = :storeId OR r.store IS NULL)")
    List<AlertRule> findActiveRulesByType(
            @Param("userId") Long userId,
            @Param("storeId") UUID storeId,
            @Param("alertType") AlertType alertType);

    /**
     * Find all active stock rules that need to be checked.
     * Returns rules where cooldown has passed since last trigger.
     */
    @Query(value = "SELECT * FROM alert_rules r WHERE r.alert_type = :alertType AND r.active = true " +
           "AND (r.last_triggered_at IS NULL OR r.last_triggered_at < CURRENT_TIMESTAMP - (r.cooldown_minutes * INTERVAL '1 minute'))",
           nativeQuery = true)
    List<AlertRule> findRulesReadyToTrigger(@Param("alertType") String alertType);

    /**
     * Find a specific rule by ID and user ID (security check).
     */
    Optional<AlertRule> findByIdAndUserId(UUID id, Long userId);

    /**
     * Count rules by user.
     */
    long countByUserId(Long userId);

    /**
     * Count active rules by user.
     */
    long countByUserIdAndActive(Long userId, Boolean active);

    /**
     * Find rules by product barcode for a specific user.
     */
    List<AlertRule> findByUserIdAndProductBarcodeAndActive(Long userId, String productBarcode, Boolean active);

    /**
     * Find rules by category for a specific user.
     */
    List<AlertRule> findByUserIdAndCategoryNameAndActive(Long userId, String categoryName, Boolean active);

    /**
     * Paginated rules for a user.
     */
    Page<AlertRule> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Check if a rule with given name already exists for a user.
     */
    boolean existsByUserIdAndName(Long userId, String name);
}
