package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailDto {
    
    private String orderNumber;
    private LocalDateTime orderDate;
    private List<OrderProductDetailDto> products;
    private BigDecimal totalPrice;
    private BigDecimal returnPrice;
    private BigDecimal revenue; // ciro
    private BigDecimal grossProfit; // brüt kar
    private BigDecimal stoppage; // stopaj
    private BigDecimal estimatedCommission; // tahmini komisyon
    private BigDecimal estimatedShippingCost; // tahmini kargo maliyeti
}
