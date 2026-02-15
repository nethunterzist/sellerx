package com.ecommerce.sellerx.orders;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TrendyolOrderMapper {

    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "isReturnShippingEstimated", expression = "java(order.getReturnShippingCost() == null || order.getReturnShippingCost().compareTo(java.math.BigDecimal.ZERO) == 0)")
    TrendyolOrderDto toDto(TrendyolOrder order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TrendyolOrder toEntity(TrendyolOrderDto dto);
}
