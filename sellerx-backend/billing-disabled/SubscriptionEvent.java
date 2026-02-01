package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription event audit trail
 */
@Entity
@Table(name = "subscription_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SubscriptionEventType eventType;

    // State change tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private SubscriptionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private SubscriptionStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_plan_id")
    private SubscriptionPlan previousPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_plan_id")
    private SubscriptionPlan newPlan;

    /**
     * Additional event data
     * Example: {"paymentId": "...", "amount": 599, "reason": "card_declined"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Create a status change event
     */
    public static SubscriptionEvent statusChange(Subscription subscription,
                                                  SubscriptionStatus previousStatus,
                                                  SubscriptionStatus newStatus,
                                                  SubscriptionEventType eventType) {
        return SubscriptionEvent.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .eventType(eventType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .build();
    }

    /**
     * Create a plan change event
     */
    public static SubscriptionEvent planChange(Subscription subscription,
                                                SubscriptionPlan previousPlan,
                                                SubscriptionPlan newPlan,
                                                SubscriptionEventType eventType) {
        return SubscriptionEvent.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .eventType(eventType)
                .previousPlan(previousPlan)
                .newPlan(newPlan)
                .build();
    }
}
