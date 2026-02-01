package com.ecommerce.sellerx.buybox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buybox anlık görüntüsü entity'si.
 * Her 12 saatlik kontrolde bir snapshot oluşturulur.
 */
@Entity
@Table(name = "buybox_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_product_id", nullable = false)
    private BuyboxTrackedProduct trackedProduct;

    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private LocalDateTime checkedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "buybox_status", nullable = false, length = 20)
    private BuyboxStatus buyboxStatus;

    @Column(name = "winner_merchant_id")
    private Long winnerMerchantId;

    @Column(name = "winner_merchant_name")
    private String winnerMerchantName;

    @Column(name = "winner_price", precision = 10, scale = 2)
    private BigDecimal winnerPrice;

    @Column(name = "winner_seller_score", precision = 3, scale = 1)
    private BigDecimal winnerSellerScore;

    @Column(name = "my_price", precision = 10, scale = 2)
    private BigDecimal myPrice;

    @Column(name = "my_position")
    private Integer myPosition;

    @Column(name = "price_difference", precision = 10, scale = 2)
    private BigDecimal priceDifference;

    @Column(name = "total_sellers")
    private Integer totalSellers;

    @Column(name = "lowest_price", precision = 10, scale = 2)
    private BigDecimal lowestPrice;

    @Column(name = "highest_price", precision = 10, scale = 2)
    private BigDecimal highestPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "competitors_json", columnDefinition = "jsonb")
    private String competitorsJson;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
