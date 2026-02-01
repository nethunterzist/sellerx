package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

/**
 * iyzico 3D Secure initialization result
 */
@Data
@Builder
public class IyzicoThreeDsResult {

    private boolean success;
    private String htmlContent;
    private String errorCode;
    private String errorMessage;

    public static IyzicoThreeDsResult failure(String errorCode, String errorMessage) {
        return IyzicoThreeDsResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
