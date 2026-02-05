package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellData {
    private String sourceBarcode;
    private String sourceProductName;
    private String targetBarcode;
    private String targetProductName;
    private int coOccurrenceCount;
    private double confidence;
    private String sourceImage;
    private String sourceProductUrl;
    private String targetImage;
    private String targetProductUrl;
}
