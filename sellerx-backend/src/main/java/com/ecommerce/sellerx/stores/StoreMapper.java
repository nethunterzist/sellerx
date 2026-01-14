package com.ecommerce.sellerx.stores;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.ecommerce.sellerx.users.User;

@Mapper(componentModel = "spring")
public interface StoreMapper {
    @Mapping(target = "userId", source = "user.id")
    StoreDto toDto(Store store);
    @Mapping(target = "user", ignore = true)
    Store toEntity(RegisterStoreRequest request);
    void update(UpdateStoreRequest request, @MappingTarget Store store);
}
