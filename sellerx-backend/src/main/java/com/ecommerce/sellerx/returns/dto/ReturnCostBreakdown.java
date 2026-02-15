package com.ecommerce.sellerx.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReturnCostBreakdown {
    private BigDecimal productCost;        // Ürün maliyeti (FIFO'dan)
    private BigDecimal shippingCostOut;    // Gidiş kargo
    private BigDecimal shippingCostReturn; // Dönüş kargo (gerçek veya tahmini)
    private BigDecimal commissionLoss;     // Komisyon kaybı
    private BigDecimal packagingCost;      // Ambalaj maliyeti
    private BigDecimal totalLoss;          // TOPLAM ZARAR

    // İade kargo tahminleme bilgisi
    private int estimatedReturnShippingCount; // Tahmini (gönderi kargosundan referans)
    private int realReturnShippingCount;      // Gerçek (iade kargo faturasından)
}
