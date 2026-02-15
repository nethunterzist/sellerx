package com.ecommerce.sellerx.returns.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReturnedOrderDto {

    private String orderNumber;
    private String customerName;
    private LocalDateTime orderDate;
    private List<ReturnedItemDto> items;
    private BigDecimal shippingCostOut;
    private BigDecimal shippingCostReturn;
    private BigDecimal productCost;
    private BigDecimal totalLoss;
    private Boolean isResalable;
    private String returnReason;     // Main return reason from claims
    private String claimStatus;      // Claim status (Created, Accepted, Rejected, etc.)
    private String returnSource;     // Detection source: "order_status", "claim", "cargo_invoice"

    @Data
    @Builder
    public static class ReturnedItemDto {
        private String barcode;
        private String productName;
        private int quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
    }
}
