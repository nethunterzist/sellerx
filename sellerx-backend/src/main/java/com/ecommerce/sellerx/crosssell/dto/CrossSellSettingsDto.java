package com.ecommerce.sellerx.crosssell.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellSettingsDto {
    private UUID id;
    private UUID storeId;
    private Boolean enabled;
    private Integer defaultMaxProducts;
    private Boolean includeInAnswer;
    private Boolean showProductImage;
    private Boolean showProductPrice;
}
