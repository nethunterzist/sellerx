package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buybox alert entity'si.
 * Buybox değişikliklerinde kullanıcıya gönderilen uyarılar.
 */
@Entity
@Table(name = "buybox_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_product_id", nullable = false)
    private BuyboxTrackedProduct trackedProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private BuyboxAlertType alertType;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "old_winner_name")
    private String oldWinnerName;

    @Column(name = "new_winner_name")
    private String newWinnerName;

    @Column(name = "price_before", precision = 10, scale = 2)
    private BigDecimal priceBefore;

    @Column(name = "price_after", precision = 10, scale = 2)
    private BigDecimal priceAfter;

    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
