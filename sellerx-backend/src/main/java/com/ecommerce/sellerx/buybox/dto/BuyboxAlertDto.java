package com.ecommerce.sellerx.buybox.dto;

import com.ecommerce.sellerx.buybox.BuyboxAlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buybox alert DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyboxAlertDto {

    private UUID id;
    private UUID storeId;
    private UUID trackedProductId;

    private BuyboxAlertType alertType;
    private String title;
    private String message;

    // Ürün bilgileri
    private String productTitle;
    private String productImageUrl;

    private String oldWinnerName;
    private String newWinnerName;
    private BigDecimal priceBefore;
    private BigDecimal priceAfter;

    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
