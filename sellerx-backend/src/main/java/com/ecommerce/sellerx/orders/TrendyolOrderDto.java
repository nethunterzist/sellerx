package com.ecommerce.sellerx.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record TrendyolOrderDto(
    UUID id,
    UUID storeId,
    
    @JsonProperty("tyOrderNumber")
    String tyOrderNumber,
    
    @JsonProperty("packageNo")
    Long packageNo,
    
    @JsonProperty("orderDate")
    LocalDateTime orderDate,
    
    @JsonProperty("grossAmount")
    BigDecimal grossAmount,
    
    @JsonProperty("totalDiscount")
    BigDecimal totalDiscount,
    
    @JsonProperty("totalTyDiscount")
    BigDecimal totalTyDiscount,
    
    @JsonProperty("totalPrice")
    BigDecimal totalPrice,
    
    @JsonProperty("stoppage")
    BigDecimal stoppage,
    
    @JsonProperty("estimatedCommission")
    BigDecimal estimatedCommission,

    @JsonProperty("estimatedShippingCost")
    BigDecimal estimatedShippingCost,

    @JsonProperty("orderItems")
    List<OrderItem> orderItems,
    
    @JsonProperty("shipmentPackageStatus")
    String shipmentPackageStatus,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("cargoDeci")
    Integer cargoDeci,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {}
