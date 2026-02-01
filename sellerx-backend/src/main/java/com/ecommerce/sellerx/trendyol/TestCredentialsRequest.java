package com.ecommerce.sellerx.trendyol;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCredentialsRequest {
    private String sellerId;
    private String apiKey;
    private String apiSecret;
}
