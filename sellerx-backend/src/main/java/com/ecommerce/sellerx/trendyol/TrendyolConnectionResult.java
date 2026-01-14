package com.ecommerce.sellerx.trendyol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendyolConnectionResult {
    private boolean connected;
    private String message;
    private String sellerId;
    private int statusCode;
}
