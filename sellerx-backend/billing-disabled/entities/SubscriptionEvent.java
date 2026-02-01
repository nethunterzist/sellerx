package com.ecommerce.sellerx.billing;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription event for audit trail
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

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SubscriptionEventType eventType;

    @Type(JsonType.class)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Create a new event
     */
    public static SubscriptionEvent of(Subscription subscription, SubscriptionEventType eventType, Map<String, Object> data) {
        return SubscriptionEvent.builder()
                .subscription(subscription)
                .eventType(eventType)
                .eventData(data)
                .build();
    }

    /**
     * Create a simple event without data
     */
    public static SubscriptionEvent of(Subscription subscription, SubscriptionEventType eventType) {
        return SubscriptionEvent.builder()
                .subscription(subscription)
                .eventType(eventType)
                .build();
    }
}
