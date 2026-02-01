package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feature configuration per plan
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

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 20)
    @Builder.Default
    private FeatureType featureType = FeatureType.BOOLEAN;

    @Column(name = "limit_value")
    private Integer limitValue;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if feature is available
     */
    public boolean isAvailable() {
        if (!isEnabled) {
            return false;
        }
        return featureType == FeatureType.BOOLEAN || featureType == FeatureType.UNLIMITED;
    }

    /**
     * Check if feature has a limit
     */
    public boolean hasLimit() {
        return featureType == FeatureType.LIMIT && limitValue != null && limitValue > 0;
    }

    /**
     * Get effective limit (-1 for unlimited)
     */
    public int getEffectiveLimit() {
        if (featureType == FeatureType.UNLIMITED) {
            return -1;
        }
        if (featureType == FeatureType.LIMIT && limitValue != null) {
            return limitValue;
        }
        return 0;
    }
}
