package com.ecommerce.sellerx.buybox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Buybox dashboard DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxDashboardDto {

    private UUID storeId;

    // Özet istatistikler
    private int totalTrackedProducts;
    private int wonCount;
    private int lostCount;
    private int riskCount;
    private int noCompetitionCount;

    // Okunmamış alert sayısı
    private int unreadAlertCount;

    // Takip edilen ürünler listesi (özet)
    private List<BuyboxTrackedProductDto> products;

    // Son alertler
    private List<BuyboxAlertDto> recentAlerts;

    private LocalDateTime lastUpdatedAt;
}
