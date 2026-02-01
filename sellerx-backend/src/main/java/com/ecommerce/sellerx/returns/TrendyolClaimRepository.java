package com.ecommerce.sellerx.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolClaimRepository extends JpaRepository<TrendyolClaim, UUID> {

    // Find by store with pagination
    Page<TrendyolClaim> findByStoreIdOrderByClaimDateDesc(UUID storeId, Pageable pageable);

    // Find by store and status with pagination
    Page<TrendyolClaim> findByStoreIdAndStatusOrderByClaimDateDesc(UUID storeId, String status, Pageable pageable);

    // Find by store and multiple statuses
    Page<TrendyolClaim> findByStoreIdAndStatusInOrderByClaimDateDesc(UUID storeId, List<String> statuses, Pageable pageable);

    // Find by store and claim ID (Trendyol's claim ID)
    Optional<TrendyolClaim> findByStoreIdAndClaimId(UUID storeId, String claimId);

    // Find by store and order number
    List<TrendyolClaim> findByStoreIdAndOrderNumber(UUID storeId, String orderNumber);

    // Find by store and date range
    @Query("SELECT c FROM TrendyolClaim c WHERE c.store.id = :storeId " +
           "AND c.claimDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.claimDate DESC")
    List<TrendyolClaim> findByStoreIdAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count by store and status
    long countByStoreIdAndStatus(UUID storeId, String status);

    // Count by store
    long countByStoreId(UUID storeId);

    // Find pending claims (WaitingInAction)
    @Query("SELECT c FROM TrendyolClaim c WHERE c.store.id = :storeId " +
           "AND c.status = 'WaitingInAction' ORDER BY c.claimDate DESC")
    List<TrendyolClaim> findPendingClaimsByStoreId(@Param("storeId") UUID storeId);

    // Check if claim exists
    boolean existsByStoreIdAndClaimId(UUID storeId, String claimId);

    // Get status distribution for store
    @Query("SELECT c.status AS status, COUNT(c) AS statusCount FROM TrendyolClaim c WHERE c.store.id = :storeId GROUP BY c.status")
    List<StatusCountProjection> getStatusDistributionByStoreId(@Param("storeId") UUID storeId);
}
