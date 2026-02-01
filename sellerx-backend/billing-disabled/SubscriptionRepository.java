package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(Long userId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan JOIN FETCH s.price WHERE s.user.id = :userId")
    Optional<Subscription> findByUserIdWithPlanAndPrice(@Param("userId") Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    List<Subscription> findByStatus(@Param("status") SubscriptionStatus status);

    // Trial management
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate <= :endDate")
    List<Subscription> findTrialsEndingBefore(@Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate BETWEEN :start AND :end")
    List<Subscription> findTrialsEndingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Renewal management
    @Query("SELECT s FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIAL') AND s.autoRenew = true AND s.cancelAtPeriodEnd = false AND s.currentPeriodEnd <= :endDate")
    List<Subscription> findSubscriptionsToRenew(@Param("endDate") LocalDateTime endDate);

    // Past due and grace period management
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.gracePeriodEnd <= :endDate")
    List<Subscription> findGracePeriodExpired(@Param("endDate") LocalDateTime endDate);

    // Suspended subscription cleanup
    @Query("SELECT s FROM Subscription s WHERE s.status = 'SUSPENDED' AND s.updatedAt <= :cutoffDate")
    List<Subscription> findSuspendedOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Downgrade scheduling
    @Query("SELECT s FROM Subscription s WHERE s.downgradeToPlan IS NOT NULL AND s.currentPeriodEnd <= :endDate")
    List<Subscription> findScheduledDowngrades(@Param("endDate") LocalDateTime endDate);

    // Statistics
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIAL', 'PAST_DUE')")
    long countActiveSubscriptions();

    @Query("SELECT s.plan.code, COUNT(s) FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIAL') GROUP BY s.plan.code")
    List<Object[]> countSubscriptionsByPlan();

    boolean existsByUserId(Long userId);
}
