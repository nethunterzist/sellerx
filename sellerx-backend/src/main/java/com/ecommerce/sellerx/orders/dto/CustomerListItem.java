package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListItem {
    private String customerKey;
    private String displayName;
    private String city;
    private int orderCount;
    private int itemCount;
    private BigDecimal totalSpend;
    private LocalDateTime firstOrderDate;
    private LocalDateTime lastOrderDate;
    private double avgOrderValue;
    private String rfmSegment;
    private int recencyScore;
    private int frequencyScore;
    private int monetaryScore;
}
