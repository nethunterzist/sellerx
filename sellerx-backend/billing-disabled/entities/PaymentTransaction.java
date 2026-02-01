package com.ecommerce.sellerx.billing;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payment transaction record
 */
@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // iyzico data
    @Column(name = "iyzico_payment_id", length = 200)
    private String iyzicoPaymentId;

    @Column(name = "iyzico_conversation_id", length = 200)
    private String iyzicoConversationId;

    @Type(JsonType.class)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private Map<String, Object> providerResponse;

    // Failure info
    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    // Retry tracking
    @Column(name = "attempt_number")
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

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
     * Check if can retry payment
     */
    public boolean canRetry() {
        return status == PaymentStatus.FAILED && attemptNumber < MAX_RETRY_ATTEMPTS;
    }

    /**
     * Schedule next retry
     * Retry schedule: Day 0 (immediate), Day 1, Day 2
     */
    public void scheduleRetry() {
        if (!canRetry()) {
            return;
        }
        int daysUntilRetry = attemptNumber; // 1 day after first fail, 2 days after second fail
        this.nextRetryAt = LocalDateTime.now().plusDays(daysUntilRetry);
        this.attemptNumber++;
    }

    /**
     * Get remaining retry attempts
     */
    public int getRemainingRetries() {
        return Math.max(0, MAX_RETRY_ATTEMPTS - attemptNumber);
    }
}
