package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Invoice record for subscription payments
 */
@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Invoice number format: INV-2026-000001
     */
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_series", nullable = false, length = 10)
    @Builder.Default
    private String invoiceSeries = "SEL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Amounts
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * KDV rate (default 20% in Turkey)
     */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("20");

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    // Billing period
    @Column(name = "billing_period_start", nullable = false)
    private LocalDateTime billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDateTime billingPeriodEnd;

    // Payment timing
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Line items stored as JSONB
     * Example: [{"description": "Pro Plan - Aylık", "quantity": 1, "unitPrice": 599, "amount": 599}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "line_items", columnDefinition = "jsonb")
    private List<Map<String, Object>> lineItems;

    /**
     * Billing address snapshot at invoice time
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Map<String, Object> billingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
     * Check if invoice is paid
     */
    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    /**
     * Check if invoice is overdue
     */
    public boolean isOverdue() {
        return status == InvoiceStatus.PENDING &&
                dueDate != null &&
                LocalDateTime.now().isAfter(dueDate);
    }

    /**
     * Calculate tax from subtotal
     */
    public void calculateTax() {
        if (subtotal != null && taxRate != null) {
            this.taxAmount = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            this.totalAmount = subtotal.add(taxAmount);
        }
    }

    /**
     * Get billing period description
     */
    public String getBillingPeriodDescription() {
        if (billingPeriodStart == null || billingPeriodEnd == null) return "";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return billingPeriodStart.format(formatter) + " - " + billingPeriodEnd.format(formatter);
    }
}
