package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopProductDto {
    private String barcode;
    private String title;
    private String storeName;
    private long orderCount;
    private BigDecimal totalRevenue;
}
