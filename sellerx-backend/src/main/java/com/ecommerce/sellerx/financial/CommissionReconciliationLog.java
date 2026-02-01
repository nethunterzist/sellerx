package com.ecommerce.sellerx.financial;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.ecommerce.sellerx.stores.Store;

/**
 * Tracks daily reconciliation between estimated (Orders API) and real (Settlement API) commissions.
 * This entity helps monitor the accuracy of commission estimations and track reconciliation progress.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "commission_reconciliation_log")
public class CommissionReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * Number of orders reconciled on this date
     */
    @Column(name = "total_reconciled")
    @Builder.Default
    private Integer totalReconciled = 0;

    /**
     * Sum of estimated commissions before reconciliation
     */
    @Column(name = "total_estimated", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEstimated = BigDecimal.ZERO;

    /**
     * Sum of real commissions from Settlement API
     */
    @Column(name = "total_real", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalReal = BigDecimal.ZERO;

    /**
     * Total difference (real - estimated)
     * Positive: Underestimated commissions
     * Negative: Overestimated commissions
     */
    @Column(name = "total_difference", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDifference = BigDecimal.ZERO;

    /**
     * Percentage accuracy of estimations (0-100)
     * Calculated as: 100 - abs(difference/real * 100)
     */
    @Column(name = "average_accuracy", precision = 5, scale = 2)
    private BigDecimal averageAccuracy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates and sets the average accuracy based on total estimated vs real commissions
     */
    public void calculateAccuracy() {
        if (totalReal != null && totalReal.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal differenceAbs = totalDifference.abs();
            BigDecimal accuracyLoss = differenceAbs
                .divide(totalReal, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            this.averageAccuracy = new BigDecimal("100").subtract(accuracyLoss);

            // Clamp to 0-100 range
            if (this.averageAccuracy.compareTo(BigDecimal.ZERO) < 0) {
                this.averageAccuracy = BigDecimal.ZERO;
            }
            if (this.averageAccuracy.compareTo(new BigDecimal("100")) > 0) {
                this.averageAccuracy = new BigDecimal("100");
            }
        } else {
            this.averageAccuracy = null;
        }
    }
}
