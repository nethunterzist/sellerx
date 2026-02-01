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
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    Optional<PaymentWebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Query("SELECT pwe FROM PaymentWebhookEvent pwe WHERE pwe.processingStatus = 'FAILED' ORDER BY pwe.createdAt DESC")
    List<PaymentWebhookEvent> findFailedEvents();

    @Query("SELECT pwe FROM PaymentWebhookEvent pwe WHERE pwe.processingStatus = 'RECEIVED' AND pwe.createdAt <= :cutoff")
    List<PaymentWebhookEvent> findStuckEvents(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT pwe FROM PaymentWebhookEvent pwe WHERE pwe.subscription.id = :subscriptionId ORDER BY pwe.createdAt DESC")
    List<PaymentWebhookEvent> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT pwe.eventType AS typeName, COUNT(pwe) AS typeCount FROM PaymentWebhookEvent pwe WHERE pwe.createdAt >= :since GROUP BY pwe.eventType")
    List<TypeCountProjection> countEventsByTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(pwe.processingTimeMs) FROM PaymentWebhookEvent pwe WHERE pwe.processingStatus = 'COMPLETED' AND pwe.createdAt >= :since")
    Double averageProcessingTimeSince(@Param("since") LocalDateTime since);
}
