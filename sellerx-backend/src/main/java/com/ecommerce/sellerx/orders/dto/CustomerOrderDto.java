package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for customer order details used in customer analytics detail panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDto {
    private String orderId;
    private String tyOrderNumber;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalPrice;
    private BigDecimal totalDiscount;
    private String shipmentCity;
    private List<CustomerOrderItemDto> items;

    /**
     * Individual order item within an order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderItemDto {
        private String barcode;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal price;  // Final price after discounts
        private String image;
        private String productUrl;
    }
}
