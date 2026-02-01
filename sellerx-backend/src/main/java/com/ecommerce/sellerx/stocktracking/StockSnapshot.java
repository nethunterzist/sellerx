package com.ecommerce.sellerx.stocktracking;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a point-in-time snapshot of a competitor's stock level.
 * Created every hour (or at configured interval) for historical tracking.
 */
@Entity
@Table(name = "stock_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_product_id", nullable = false)
    private StockTrackedProduct trackedProduct;

    // Stock data
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    // Change tracking
    @Column(name = "previous_quantity")
    private Integer previousQuantity;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Column(name = "checked_at")
    @Builder.Default
    private LocalDateTime checkedAt = LocalDateTime.now();

    // Helper to calculate change
    public static StockSnapshot create(StockTrackedProduct product, int quantity, boolean inStock, BigDecimal price) {
        Integer previousQty = product.getLastStockQuantity();
        Integer change = previousQty != null ? quantity - previousQty : null;

        return StockSnapshot.builder()
                .trackedProduct(product)
                .quantity(quantity)
                .inStock(inStock)
                .price(price)
                .previousQuantity(previousQty)
                .quantityChange(change)
                .checkedAt(LocalDateTime.now())
                .build();
    }
}
