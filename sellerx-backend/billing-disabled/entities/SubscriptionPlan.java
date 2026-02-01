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
 * Subscription plan definition
 */
@Entity
@Table(name = "subscription_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Maximum number of stores allowed (null = unlimited)
     */
    @Column(name = "max_stores")
    private Integer maxStores;

    /**
     * Feature configuration as JSON
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if plan is free
     */
    public boolean isFree() {
        return "FREE".equals(code);
    }

    /**
     * Check if stores are unlimited
     */
    public boolean hasUnlimitedStores() {
        return maxStores == null;
    }

    /**
     * Get feature value
     */
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String featureCode, T defaultValue) {
        if (features == null) {
            return defaultValue;
        }
        Object value = features.get(featureCode);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
}
