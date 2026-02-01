package com.ecommerce.sellerx.webhook;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_event_id", columnList = "event_id"),
    @Index(name = "idx_webhook_store_created", columnList = "store_id, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum ProcessingStatus {
        RECEIVED,
        PROCESSING,
        COMPLETED,
        FAILED,
        DUPLICATE
    }
}
