package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecentOrderDto {
    private UUID id;
    private String orderNumber;
    private String storeName;
    private String status;
    private BigDecimal totalPrice;
    private LocalDateTime orderDate;
}
