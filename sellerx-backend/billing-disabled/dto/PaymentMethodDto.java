package com.ecommerce.sellerx.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Payment method DTO for API responses
 */
@Data
@Builder
public class PaymentMethodDto {

    private UUID id;
    private String type;
    private String cardLastFour;
    private String cardBrand;
    private String cardFamily;
    private String cardHolderName;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String cardBankName;
    private Boolean isDefault;
    private Boolean isExpired;
    private String maskedNumber;
    private String expirationDisplay;
}
