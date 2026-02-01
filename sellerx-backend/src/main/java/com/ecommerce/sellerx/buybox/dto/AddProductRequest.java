package com.ecommerce.sellerx.buybox.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Buybox takibine ürün ekleme isteği DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddProductRequest {

    @NotNull(message = "Ürün ID zorunludur")
    private UUID productId;
}
