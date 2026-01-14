package com.ecommerce.sellerx.stores;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RegisterStoreRequest {
    @NotBlank
    private String storeName;
    @NotBlank
    private String marketplace;
    @NotNull
    private MarketplaceCredentials credentials;
}
