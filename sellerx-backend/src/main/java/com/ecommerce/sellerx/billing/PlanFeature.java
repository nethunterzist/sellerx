package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feature configuration per subscription plan
 */
@Entity
@Table(name = "plan_features",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "feature_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 20)
    private FeatureType featureType;

    /**
     * Limit value (for LIMIT type features)
     * NULL for BOOLEAN and UNLIMITED types
     */
    @Column(name = "limit_value")
    private Integer limitValue;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
     * Check if feature is available
     */
    public boolean isAvailable() {
        return isEnabled;
    }

    /**
     * Check if feature has a limit
     */
    public boolean hasLimit() {
        return featureType == FeatureType.LIMIT && limitValue != null;
    }

    /**
     * Check if feature is unlimited
     */
    public boolean isUnlimited() {
        return featureType == FeatureType.UNLIMITED;
    }

    /**
     * Get effective limit (returns Integer.MAX_VALUE for unlimited)
     */
    public int getEffectiveLimit() {
        if (featureType == FeatureType.UNLIMITED) {
            return Integer.MAX_VALUE;
        }
        return limitValue != null ? limitValue : 0;
    }
}
