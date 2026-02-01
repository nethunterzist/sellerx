package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

/**
 * iyzico card storage request DTO
 */
@Data
@Builder
public class IyzicoCardRequest {

    // User identification (for existing user)
    private String cardUserKey;

    // User identification (for new user)
    private String email;
    private String externalId;

    // Card details
    private String cardAlias;
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
}
