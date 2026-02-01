package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCostUpdateResponse {
    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private List<FailedItem> failedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String barcode;
        private String reason;
    }
}
