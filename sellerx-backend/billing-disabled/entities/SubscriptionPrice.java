package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pricing for subscription plans by billing cycle
 */
@Entity
@Table(name = "subscription_prices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "billing_cycle"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

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
     * Get monthly equivalent price
     */
    public BigDecimal getMonthlyPrice() {
        return priceAmount.divide(BigDecimal.valueOf(billingCycle.getMonths()), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get savings compared to monthly
     */
    public BigDecimal getSavingsAmount(BigDecimal monthlyPrice) {
        BigDecimal fullPrice = monthlyPrice.multiply(BigDecimal.valueOf(billingCycle.getMonths()));
        return fullPrice.subtract(priceAmount);
    }
}
