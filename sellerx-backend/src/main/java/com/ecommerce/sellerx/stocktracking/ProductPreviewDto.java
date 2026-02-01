package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for product preview before adding to tracking.
 * Used to show product info to user for verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPreviewDto {
    private Long productId;
    private String productName;
    private String brandName;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private Boolean inStock;
    private Boolean isValid;
    private String errorMessage;

    /**
     * Create a preview from StockData
     */
    public static ProductPreviewDto fromStockData(TrendyolPublicStockClient.StockData stockData) {
        if (stockData == null) {
            return ProductPreviewDto.builder()
                    .isValid(false)
                    .errorMessage("Ürün bilgileri alınamadı")
                    .build();
        }

        return ProductPreviewDto.builder()
                .productId(stockData.getProductId())
                .productName(stockData.getProductName())
                .brandName(stockData.getBrandName())
                .imageUrl(stockData.getImageUrl())
                .price(stockData.getPrice())
                .quantity(stockData.getQuantity())
                .inStock(stockData.getInStock())
                .isValid(stockData.getProductId() != null && stockData.getProductName() != null)
                .build();
    }

    /**
     * Create an invalid preview response
     */
    public static ProductPreviewDto invalid(String errorMessage) {
        return ProductPreviewDto.builder()
                .isValid(false)
                .errorMessage(errorMessage)
                .build();
    }
}
