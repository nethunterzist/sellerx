package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.products.TrendyolProduct;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private TrendyolProduct product;

    @Column(name = "units_ordered", nullable = false)
    private Integer unitsOrdered;

    @Column(name = "units_per_box")
    private Integer unitsPerBox;

    @Column(name = "boxes_ordered")
    private Integer boxesOrdered;

    @Column(name = "box_dimensions", length = 100)
    private String boxDimensions;

    @Column(name = "manufacturing_cost_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal manufacturingCostPerUnit;

    @Column(name = "transportation_cost_per_unit", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal transportationCostPerUnit = BigDecimal.ZERO;

    // Computed column in database - read only
    @Column(name = "total_cost_per_unit", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalCostPerUnit;

    // VAT rate for cost (default 20% for Turkey)
    @Column(name = "cost_vat_rate")
    @Builder.Default
    private Integer costVatRate = 20;

    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @Column(name = "manufacturing_cost_supplier_currency", precision = 10, scale = 2)
    private BigDecimal manufacturingCostSupplierCurrency;

    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels;

    @Column(name = "stock_entry_date")
    private LocalDate stockEntryDate;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Helper method to calculate total cost for this item
    public BigDecimal getTotalCost() {
        BigDecimal costPerUnit = manufacturingCostPerUnit.add(
            transportationCostPerUnit != null ? transportationCostPerUnit : BigDecimal.ZERO
        );
        return costPerUnit.multiply(BigDecimal.valueOf(unitsOrdered));
    }
}
