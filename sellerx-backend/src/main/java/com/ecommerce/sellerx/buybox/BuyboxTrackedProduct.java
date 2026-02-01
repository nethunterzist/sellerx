package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buybox takibi için seçilmiş ürün entity'si.
 * Müşteriler kendi envanterlerinden maksimum 10 ürün seçebilir.
 */
@Entity
@Table(name = "buybox_tracked_products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxTrackedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private TrendyolProduct product;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "alert_on_loss")
    @Builder.Default
    private boolean alertOnLoss = true;

    @Column(name = "alert_on_new_competitor")
    @Builder.Default
    private boolean alertOnNewCompetitor = true;

    @Column(name = "alert_price_threshold", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal alertPriceThreshold = new BigDecimal("10.00");

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
}
