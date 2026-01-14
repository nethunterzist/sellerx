package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncProductsResponse {
    private boolean success;
    private String message;
    private int totalFetched;
    private int totalSaved;
    private int totalUpdated;
    private int totalSkipped;
}
