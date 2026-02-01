package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payment transaction history with iyzico integration
 */
@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

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

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "iyzico";

    // iyzico transaction details
    @Column(name = "iyzico_payment_id")
    private String iyzicoPaymentId;

    /**
     * Idempotency key for iyzico requests
     */
    @Column(name = "iyzico_conversation_id")
    private String iyzicoConversationId;

    @Column(name = "iyzico_payment_transaction_id")
    private String iyzicoPaymentTransactionId;

    /**
     * Full provider response for debugging
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private Map<String, Object> providerResponse;

    // Error tracking
    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    // Retry tracking
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

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
     * Check if payment was successful
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    /**
     * Check if payment failed
     */
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    /**
     * Check if payment can be retried
     * Max 3 attempts allowed
     */
    public boolean canRetry() {
        return status == PaymentStatus.FAILED && attemptNumber < 3;
    }

    /**
     * Increment attempt number and set next retry time
     */
    public void scheduleRetry() {
        attemptNumber++;
        // Retry schedule: Day 0 (immediate), Day 1, Day 2
        nextRetryAt = LocalDateTime.now().plusDays(1);
    }
}
