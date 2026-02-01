package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

/**
 * Stored card info DTO
 */
@Data
@Builder
public class IyzicoCardInfo {

    private String cardToken;
    private String cardAlias;
    private String binNumber;
    private String lastFourDigits;
    private String cardType;
    private String cardAssociation;
    private String cardFamily;
    private String cardBankName;

    /**
     * Get masked card number for display
     */
    public String getMaskedNumber() {
        return "**** **** **** " + lastFourDigits;
    }
}
