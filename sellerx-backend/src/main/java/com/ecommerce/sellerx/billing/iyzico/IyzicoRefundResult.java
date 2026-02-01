package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * iyzico refund result DTO
 */
@Data
@Builder
public class IyzicoRefundResult {

    private boolean success;
    private String paymentId;
    private String paymentTransactionId;
    private BigDecimal price;
    private String errorCode;
    private String errorMessage;

    public static IyzicoRefundResult failure(String errorCode, String errorMessage) {
        return IyzicoRefundResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
