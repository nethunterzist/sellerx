package com.ecommerce.sellerx.products;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TrendyolProductMapper {
    
    public TrendyolProductDto toDto(TrendyolProduct product) {
        if (product == null) {
            return null;
        }
        
        return TrendyolProductDto.builder()
                .id(product.getId())
                .storeId(product.getStore().getId())
                .productId(product.getProductId())
                .barcode(product.getBarcode())
                .title(product.getTitle())
                .categoryName(product.getCategoryName())
                .createDateTime(product.getCreateDateTime())
                .hasActiveCampaign(product.getHasActiveCampaign())
                .brand(product.getBrand())
                .brandId(product.getBrandId())
                .pimCategoryId(product.getPimCategoryId())
                .productMainId(product.getProductMainId())
                .image(product.getImage())
                .productUrl(product.getProductUrl())
                .dimensionalWeight(product.getDimensionalWeight())
                .salePrice(product.getSalePrice())
                .vatRate(product.getVatRate())
                .trendyolQuantity(product.getTrendyolQuantity())
                // Prefer lastCommissionRate (from Financial API) over commissionRate (from category)
                .commissionRate(product.getLastCommissionRate() != null
                        ? product.getLastCommissionRate()
                        : product.getCommissionRate())
                .shippingVolumeWeight(product.getShippingVolumeWeight())
                .approved(product.getApproved())
                .archived(product.getArchived())
                .blacklisted(product.getBlacklisted())
                .rejected(product.getRejected())
                .onSale(product.getOnSale())
                .costAndStockInfo(product.getCostAndStockInfo())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
    
    public List<TrendyolProductDto> toDtoList(List<TrendyolProduct> products) {
        return products.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
