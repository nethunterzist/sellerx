package com.ecommerce.sellerx.purchasing;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    private PurchaseOrderStatus status;
}
