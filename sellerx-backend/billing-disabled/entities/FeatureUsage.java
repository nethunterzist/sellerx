package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feature usage tracking per billing period
 */
@Entity
@Table(name = "feature_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feature_code", "period_start"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

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
     * Increment usage count
     */
    public void incrementUsage() {
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if usage is within limit
     */
    public boolean isWithinLimit(int limit) {
        if (limit < 0) { // Unlimited
            return true;
        }
        return usageCount < limit;
    }

    /**
     * Get remaining usage
     */
    public int getRemainingUsage(int limit) {
        if (limit < 0) { // Unlimited
            return Integer.MAX_VALUE;
        }
        return Math.max(0, limit - usageCount);
    }

    /**
     * Get usage percentage
     */
    public double getUsagePercentage(int limit) {
        if (limit <= 0) {
            return 0;
        }
        return (double) usageCount / limit * 100;
    }
}
