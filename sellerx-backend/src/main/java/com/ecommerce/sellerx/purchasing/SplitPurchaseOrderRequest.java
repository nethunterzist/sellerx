package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitPurchaseOrderRequest {
    private List<Long> itemIds;
}
