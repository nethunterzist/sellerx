package com.ecommerce.sellerx.purchasing.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepletedProductDto {
    private UUID productId;
    private String productName;
    private String barcode;
    private String productImage;
    private LocalDate lastStockDate;
}
