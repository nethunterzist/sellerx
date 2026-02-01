package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * iyzico payment result DTO
 */
@Data
@Builder
public class IyzicoPaymentResult {

    private boolean success;
    private String paymentId;
    private String conversationId;
    private String paymentTransactionId;
    private BigDecimal paidPrice;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> rawResponse;

    public static IyzicoPaymentResult failure(String errorCode, String errorMessage) {
        return IyzicoPaymentResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
