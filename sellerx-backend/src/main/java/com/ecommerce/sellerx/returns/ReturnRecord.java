package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private TrendyolOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "barcode", nullable = false, length = 100)
    private String barcode;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Zarar kırılımı
    @Column(name = "product_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal productCost = BigDecimal.ZERO;

    @Column(name = "shipping_cost_out", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCostOut = BigDecimal.ZERO;

    @Column(name = "shipping_cost_return", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCostReturn = BigDecimal.ZERO;

    @Column(name = "commission_loss", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal commissionLoss = BigDecimal.ZERO;

    @Column(name = "packaging_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal packagingCost = BigDecimal.ZERO;

    @Column(name = "total_loss", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalLoss = BigDecimal.ZERO;

    // İade bilgileri
    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "return_reason_code", length = 50)
    private String returnReasonCode;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "return_status", length = 50)
    @Builder.Default
    private String returnStatus = "RECEIVED";

    /**
     * Whether the returned product can be resold.
     * null = decision pending (default: only shipping costs counted as loss)
     * true = resalable (only shipping costs as loss)
     * false = not resalable (shipping + product cost as loss)
     */
    @Column(name = "is_resalable")
    private Boolean isResalable;

    // Hakediş kontrolü
    @Column(name = "commission_refunded")
    @Builder.Default
    private Boolean commissionRefunded = false;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total loss from all cost components
     */
    public void calculateTotalLoss() {
        // Trendyol iade durumunda komisyonu geri veriyor, totalLoss'a dahil etme
        // Ürün maliyeti sadece isResalable == false ise dahil edilir (satılamaz kararı)
        this.totalLoss = BigDecimal.ZERO
                .add(shippingCostOut != null ? shippingCostOut : BigDecimal.ZERO)
                .add(shippingCostReturn != null ? shippingCostReturn : BigDecimal.ZERO)
                .add(packagingCost != null ? packagingCost : BigDecimal.ZERO);
        if (Boolean.FALSE.equals(isResalable) && productCost != null) {
            this.totalLoss = this.totalLoss.add(productCost);
        }
    }
}
