package com.ecommerce.sellerx.stores;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;

@AllArgsConstructor
@Getter
public class StoreDto {
    private java.util.UUID id;
    private Long userId;
    private String storeName;
    private String marketplace;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MarketplaceCredentials credentials;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
