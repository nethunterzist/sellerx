package com.ecommerce.sellerx.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TopReturnedProduct {
    private String barcode;
    private String productName;
    private String imageUrl;
    private int returnCount;
    private int soldCount;
    private BigDecimal returnRate; // %
    private BigDecimal totalLoss;
    private String riskLevel; // "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"
    private List<String> topReasons;
}
