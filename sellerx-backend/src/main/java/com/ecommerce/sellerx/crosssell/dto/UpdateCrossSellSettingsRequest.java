package com.ecommerce.sellerx.crosssell.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCrossSellSettingsRequest {
    private Boolean enabled;
    private Integer defaultMaxProducts;
    private Boolean includeInAnswer;
    private Boolean showProductImage;
    private Boolean showProductPrice;
}
