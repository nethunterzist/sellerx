package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDetailDto {

    private String barcode;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;      // Birim fiyat (price / quantity)
    private BigDecimal totalPrice;     // Toplam satış (price)
    private BigDecimal cost;           // Birim maliyet
    private BigDecimal totalCost;      // Toplam maliyet (cost * quantity)
    private BigDecimal commission;     // Komisyon
    private BigDecimal profit;         // Brüt kar (totalPrice - totalCost - commission)
}
