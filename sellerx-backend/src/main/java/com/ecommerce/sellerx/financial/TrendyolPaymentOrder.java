package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.stores.Store;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "trendyol_payment_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrendyolPaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "payment_order_id", nullable = false)
    private Long paymentOrderId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expected_amount", precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "discrepancy_amount", precision = 15, scale = 2)
    private BigDecimal discrepancyAmount;

    @Column(name = "discrepancy_status")
    @Builder.Default
    private String discrepancyStatus = "PENDING";

    @Column(name = "settlement_count")
    @Builder.Default
    private Integer settlementCount = 0;

    // Breakdown fields for detailed analysis
    @Column(name = "sale_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saleAmount = BigDecimal.ZERO;

    @Column(name = "sale_count")
    @Builder.Default
    private Integer saleCount = 0;

    @Column(name = "return_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal returnAmount = BigDecimal.ZERO;

    @Column(name = "return_count")
    @Builder.Default
    private Integer returnCount = 0;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal couponAmount = BigDecimal.ZERO;

    @Column(name = "commission_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "cargo_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cargoAmount = BigDecimal.ZERO;

    @Column(name = "stoppage_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal stoppageAmount = BigDecimal.ZERO;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Verify the payment order against expected amount
     * Sets discrepancy amount and status
     */
    public void verify() {
        if (this.expectedAmount != null && this.totalAmount != null) {
            this.discrepancyAmount = this.expectedAmount.subtract(this.totalAmount);

            if (this.discrepancyAmount.abs().compareTo(new BigDecimal("0.01")) <= 0) {
                this.discrepancyStatus = "MATCHED";
            } else if (this.discrepancyAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.discrepancyStatus = "UNDERPAID";
            } else {
                this.discrepancyStatus = "OVERPAID";
            }

            this.verifiedAt = LocalDateTime.now();
        }
    }
}
