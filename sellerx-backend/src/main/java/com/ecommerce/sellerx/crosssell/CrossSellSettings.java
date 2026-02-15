package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cross_sell_settings", indexes = {
    @Index(name = "idx_cross_sell_settings_store", columnList = "store_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "default_max_products", nullable = false)
    @Builder.Default
    private Integer defaultMaxProducts = 3;

    @Column(name = "include_in_answer", nullable = false)
    @Builder.Default
    private Boolean includeInAnswer = true;

    @Column(name = "show_product_image", nullable = false)
    @Builder.Default
    private Boolean showProductImage = true;

    @Column(name = "show_product_price", nullable = false)
    @Builder.Default
    private Boolean showProductPrice = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
