package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSandboxProductRequest {

    // === Temel Bilgiler (Zorunlu) ===
    private String barcode;
    private String title;
    private String brand;
    private Long brandId;
    private String categoryName;
    private Long pimCategoryId;

    // === Fiyat & Stok (Zorunlu) ===
    private BigDecimal salePrice;
    private BigDecimal listPrice;
    private Integer vatRate;
    private Integer quantity;

    // === Ürün Detayları (Opsiyonel) ===
    private String productMainId;
    private BigDecimal dimensionalWeight;
    private String image;
    private String productUrl;
    private String stockCode;
    private String color;
    private String size;
    private String gender;
    private String description;

    // === Durum Bilgileri (default değerleri frontend'de ayarlanacak) ===
    private Boolean approved;
    private Boolean onSale;
    private Boolean archived;
    private Boolean rejected;
    private Boolean blacklisted;
    private Boolean hasActiveCampaign;
}
