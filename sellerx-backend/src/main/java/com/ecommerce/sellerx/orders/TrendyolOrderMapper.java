package com.ecommerce.sellerx.orders;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TrendyolOrderMapper {
    
    @Mapping(target = "storeId", source = "store.id")
    TrendyolOrderDto toDto(TrendyolOrder order);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TrendyolOrder toEntity(TrendyolOrderDto dto);
}
