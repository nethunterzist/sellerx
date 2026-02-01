package com.ecommerce.sellerx.stocktracking;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a competitor's product being tracked for stock changes.
 * Similar to BuyboxTrackedProduct but focused on inventory levels.
 */
@Entity
@Table(name = "stock_tracked_products",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_stock_tracked_store_product",
           columnNames = {"store_id", "trendyol_product_id"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTrackedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // Trendyol product info (competitor's product)
    @Column(name = "trendyol_product_id", nullable = false)
    private Long trendyolProductId;

    @Column(name = "product_url", nullable = false, length = 500)
    private String productUrl;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "brand_name", length = 200)
    private String brandName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Tracking settings
    @Column(name = "check_interval_hours")
    @Builder.Default
    private Integer checkIntervalHours = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Alert settings
    @Column(name = "alert_on_out_of_stock")
    @Builder.Default
    private Boolean alertOnOutOfStock = true;

    @Column(name = "alert_on_low_stock")
    @Builder.Default
    private Boolean alertOnLowStock = true;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "alert_on_stock_increase")
    @Builder.Default
    private Boolean alertOnStockIncrease = false;

    @Column(name = "alert_on_back_in_stock")
    @Builder.Default
    private Boolean alertOnBackInStock = true;

    // Current state
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_stock_quantity")
    private Integer lastStockQuantity;

    @Column(name = "last_price", precision = 15, scale = 2)
    private BigDecimal lastPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean needsCheck() {
        if (!isActive || lastCheckedAt == null) {
            return true;
        }
        return lastCheckedAt.plusHours(checkIntervalHours).isBefore(LocalDateTime.now());
    }
}
