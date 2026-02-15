package com.ecommerce.sellerx.products.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record BuyboxInfoDto(
    UUID productId,
    String barcode,
    String title,
    String image,
    String productUrl,
    BigDecimal salePrice,
    Integer buyboxOrder,
    BigDecimal buyboxPrice,
    Boolean hasMultipleSeller,
    BigDecimal priceDifference,
    String buyboxStatus,
    LocalDateTime buyboxUpdatedAt
) {}
