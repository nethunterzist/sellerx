package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * iyzico webhook event for idempotency
 */
@Entity
@Table(name = "payment_webhook_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * iyzico event identifier (unique)
     */
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    // Related entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private PaymentTransaction payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    /**
     * Raw webhook payload
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Mark as processing started
     */
    public void markProcessing() {
        this.processingStatus = WebhookProcessingStatus.PROCESSING;
    }

    /**
     * Mark as successfully processed
     */
    public void markCompleted(long processingTimeMs) {
        this.processingStatus = WebhookProcessingStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Mark as failed
     */
    public void markFailed(String errorMessage) {
        this.processingStatus = WebhookProcessingStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Mark as duplicate
     */
    public void markDuplicate() {
        this.processingStatus = WebhookProcessingStatus.DUPLICATE;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Check if already processed
     */
    public boolean isAlreadyProcessed() {
        return processingStatus == WebhookProcessingStatus.COMPLETED ||
                processingStatus == WebhookProcessingStatus.DUPLICATE;
    }
}
