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
 * Pricing for subscription plans with billing cycle options
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

    /**
     * Discount percentage for longer billing cycles
     * 0 for monthly, 10 for quarterly, 20 for semiannual
     */
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
     * Calculate monthly equivalent price
     */
    public BigDecimal getMonthlyEquivalent() {
        return priceAmount.divide(BigDecimal.valueOf(billingCycle.getMonths()), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate original price before discount
     */
    public BigDecimal getOriginalPrice() {
        if (discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return priceAmount;
        }
        // originalPrice = priceAmount / (1 - discount/100)
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100)));
        return priceAmount.divide(discountMultiplier, 2, java.math.RoundingMode.HALF_UP);
    }
}
