package com.ecommerce.sellerx.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, UUID> {

    List<SubscriptionEvent> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    Page<SubscriptionEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT se FROM SubscriptionEvent se WHERE se.subscription.id = :subscriptionId AND se.eventType = :eventType ORDER BY se.createdAt DESC")
    List<SubscriptionEvent> findBySubscriptionAndEventType(@Param("subscriptionId") UUID subscriptionId, @Param("eventType") SubscriptionEventType eventType);

    @Query("SELECT se FROM SubscriptionEvent se WHERE se.eventType = :eventType AND se.createdAt >= :since ORDER BY se.createdAt DESC")
    List<SubscriptionEvent> findByEventTypeSince(@Param("eventType") SubscriptionEventType eventType, @Param("since") LocalDateTime since);

    @Query("SELECT se.eventType, COUNT(se) FROM SubscriptionEvent se WHERE se.createdAt >= :since GROUP BY se.eventType")
    List<Object[]> countEventsByTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT se FROM SubscriptionEvent se WHERE se.subscription.id = :subscriptionId ORDER BY se.createdAt DESC LIMIT 1")
    SubscriptionEvent findLatestBySubscription(@Param("subscriptionId") UUID subscriptionId);
}
