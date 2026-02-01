package com.ecommerce.sellerx.buybox.dto;

import com.ecommerce.sellerx.buybox.BuyboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buybox snapshot DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxSnapshotDto {

    private UUID id;
    private LocalDateTime checkedAt;
    private BuyboxStatus buyboxStatus;

    private Long winnerMerchantId;
    private String winnerMerchantName;
    private BigDecimal winnerPrice;
    private BigDecimal winnerSellerScore;

    private BigDecimal myPrice;
    private Integer myPosition;
    private BigDecimal priceDifference;

    private Integer totalSellers;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
}
