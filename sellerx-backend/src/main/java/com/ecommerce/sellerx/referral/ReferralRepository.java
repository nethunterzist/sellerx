package com.ecommerce.sellerx.referral;

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
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    List<Referral> findByReferrerUserIdOrderByCreatedAtDesc(Long referrerUserId);

    Optional<Referral> findByReferredUserId(Long referredUserId);

    boolean existsByReferrerUserIdAndReferredUserId(Long referrerUserId, Long referredUserId);

    long countByReferrerUserIdAndStatus(Long referrerUserId, ReferralStatus status);

    long countByReferrerUserId(Long referrerUserId);

    @Query("SELECT COALESCE(SUM(r.rewardDaysGranted), 0) FROM Referral r " +
            "WHERE r.referrerUser.id = :userId AND r.status = 'COMPLETED'")
    int totalRewardDaysByReferrerId(@Param("userId") Long userId);

    // --- Admin queries ---

    Page<Referral> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReferralStatus status);

    @Query("SELECT COALESCE(SUM(r.rewardDaysGranted), 0) FROM Referral r WHERE r.status = 'COMPLETED'")
    long sumAllRewardDays();

    @Query("SELECT r.referrerUser.id AS userId, r.referrerUser.email AS email, COUNT(r) AS referralCount, " +
            "COALESCE(SUM(r.rewardDaysGranted), 0) AS totalRewardDays " +
            "FROM Referral r GROUP BY r.referrerUser.id, r.referrerUser.email " +
            "ORDER BY COUNT(r) DESC")
    List<TopReferrerProjection> findTopReferrers(Pageable pageable);
}
