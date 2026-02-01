package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

/**
 * iyzico card storage result DTO
 */
@Data
@Builder
public class IyzicoCardResult {

    private boolean success;
    private String cardUserKey;
    private String cardToken;
    private String cardAlias;
    private String binNumber;
    private String lastFourDigits;
    private String cardType;
    private String cardAssociation;
    private String cardFamily;
    private String cardBankName;
    private String errorCode;
    private String errorMessage;

    public static IyzicoCardResult failure(String errorCode, String errorMessage) {
        return IyzicoCardResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
