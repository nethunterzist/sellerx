package com.ecommerce.sellerx.products;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TrendyolProductMapper {

    public TrendyolProductDto toDto(TrendyolProduct product) {
        if (product == null) {
            return null;
        }

        // Calculate advertising metrics
        BigDecimal advertisingCostPerSale = calculateAdvertisingCostPerSale(product);
        BigDecimal acos = calculateAcos(product, advertisingCostPerSale);

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
                // Reklam Metrikleri
                .cpc(product.getCpc())
                .cvr(product.getCvr())
                .advertisingCostPerSale(advertisingCostPerSale)
                .acos(acos)
                // Döviz Kuru
                .defaultCurrency(product.getDefaultCurrency())
                .defaultExchangeRate(product.getDefaultExchangeRate())
                // ÖTV
                .otvRate(product.getOtvRate())
                // Kargo Maliyeti
                .lastShippingCostPerUnit(product.getLastShippingCostPerUnit())
                // Buybox
                .buyboxOrder(product.getBuyboxOrder())
                .buyboxPrice(product.getBuyboxPrice())
                .hasMultipleSeller(product.getHasMultipleSeller())
                .buyboxUpdatedAt(product.getBuyboxUpdatedAt())
                .approved(product.getApproved())
                .archived(product.getArchived())
                .blacklisted(product.getBlacklisted())
                .rejected(product.getRejected())
                .onSale(product.getOnSale())
                .costAndStockInfo(product.getCostAndStockInfo())
                .hasAutoDetectedCost(hasAutoDetectedCost(product))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Calculate advertising cost per sale: CPC / CVR
     * Excel formula: C19 = C23 / C24
     */
    private BigDecimal calculateAdvertisingCostPerSale(TrendyolProduct product) {
        if (product.getCpc() != null && product.getCvr() != null
                && product.getCvr().compareTo(BigDecimal.ZERO) > 0) {
            return product.getCpc().divide(product.getCvr(), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    /**
     * Calculate ACOS (Advertising Cost of Sale): advertisingCostPerSale / salePrice
     * Excel formula: C22 = C19 / C14
     */
    private BigDecimal calculateAcos(TrendyolProduct product, BigDecimal advertisingCostPerSale) {
        if (advertisingCostPerSale != null && product.getSalePrice() != null
                && product.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            return advertisingCostPerSale
                    .divide(product.getSalePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }
    
    public List<TrendyolProductDto> toDtoList(List<TrendyolProduct> products) {
        return products.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private boolean hasAutoDetectedCost(TrendyolProduct product) {
        return product.getCostAndStockInfo() != null &&
                product.getCostAndStockInfo().stream()
                        .anyMatch(c -> "AUTO_DETECTED".equals(c.getCostSource()));
    }
}
