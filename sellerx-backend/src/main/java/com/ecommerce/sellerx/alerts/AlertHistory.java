package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a triggered alert in the user's notification history.
 * These are the actual alerts shown to users in the notification center.
 */
@Entity
@Table(name = "alert_history", indexes = {
    @Index(name = "idx_alert_history_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AlertRule rule; // Can be null if rule was deleted

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // Alert details
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    // Related data (product info, values, etc.)
    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    // Delivery status
    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "push_sent")
    @Builder.Default
    private Boolean pushSent = false;

    @Column(name = "in_app_sent")
    @Builder.Default
    private Boolean inAppSent = true;

    // Approval status: INFO (default), PENDING_APPROVAL, APPROVED, DISMISSED
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "INFO";

    // Read status
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Check if this alert has been read.
     */
    public boolean isRead() {
        return readAt != null;
    }

    /**
     * Mark this alert as read.
     */
    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
