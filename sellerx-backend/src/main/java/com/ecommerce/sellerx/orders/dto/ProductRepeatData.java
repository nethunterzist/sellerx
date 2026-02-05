package com.ecommerce.sellerx.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRepeatData {
    private String barcode;
    private String productName;
    private int totalBuyers;
    private int repeatBuyers;
    private double repeatRate;
    private double avgDaysBetweenRepurchase;
    private int totalQuantitySold;
    private String image;
    private String productUrl;
}
