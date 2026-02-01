package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user-defined alert rule.
 * Users can create rules to be notified when certain conditions are met
 * (e.g., stock below threshold, profit margin drops, new orders).
 */
@Entity
@Table(name = "alert_rules", indexes = {
    @Index(name = "idx_alert_rules_user_active", columnList = "user_id, active"),
    @Index(name = "idx_alert_rules_type", columnList = "alert_type, active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store; // NULL = applies to all user's stores

    // Rule definition
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 50)
    private AlertConditionType conditionType;

    @Column(name = "threshold", precision = 15, scale = 2)
    private BigDecimal threshold;

    // Optional scope filters
    @Column(name = "product_barcode", length = 100)
    private String productBarcode; // NULL = all products

    @Column(name = "category_name", length = 200)
    private String categoryName; // NULL = all categories

    // Notification channels
    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "push_enabled")
    @Builder.Default
    private Boolean pushEnabled = false;

    @Column(name = "in_app_enabled")
    @Builder.Default
    private Boolean inAppEnabled = true;

    // Rule status
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    // Cooldown to prevent spam
    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 60;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the rule is ready to trigger (not in cooldown period).
     */
    public boolean canTrigger() {
        if (!active) {
            return false;
        }
        if (lastTriggeredAt == null) {
            return true;
        }
        return lastTriggeredAt.plusMinutes(cooldownMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * Record that this rule was triggered.
     */
    public void recordTrigger() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount = (this.triggerCount == null ? 0 : this.triggerCount) + 1;
    }
}
