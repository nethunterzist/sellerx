package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Invoice for subscription payments
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

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Amounts
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("20.00");

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TRY";

    // Billing period
    @Column(name = "billing_period_start", nullable = false)
    private LocalDateTime billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDateTime billingPeriodEnd;

    // Invoice details
    @Type(JsonType.class)
    @Column(name = "line_items", columnDefinition = "jsonb")
    private List<Map<String, Object>> lineItems;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
     * Check if invoice is paid
     */
    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    /**
     * Check if invoice is overdue
     */
    public boolean isOverdue() {
        if (status != InvoiceStatus.PENDING) {
            return false;
        }
        if (dueDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(dueDate);
    }

    /**
     * Calculate tax amount from subtotal
     */
    public void calculateTax() {
        if (subtotal != null && taxRate != null) {
            this.taxAmount = subtotal.multiply(taxRate.divide(new BigDecimal("100")));
            this.totalAmount = subtotal.add(this.taxAmount);
        }
    }

    /**
     * Generate invoice number
     */
    public static String generateInvoiceNumber() {
        String prefix = "SEL";
        String date = java.time.format.DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now());
        String random = String.format("%04d", new java.util.Random().nextInt(10000));
        return prefix + "-" + date + "-" + random;
    }
}
