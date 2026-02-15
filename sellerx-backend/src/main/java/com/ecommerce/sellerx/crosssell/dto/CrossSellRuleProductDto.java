package com.ecommerce.sellerx.crosssell.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellRuleProductDto {
    private UUID id;
    private String productBarcode;
    private Integer displayOrder;
}
