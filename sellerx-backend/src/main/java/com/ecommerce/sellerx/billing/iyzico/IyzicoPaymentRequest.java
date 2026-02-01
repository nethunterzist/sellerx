package com.ecommerce.sellerx.billing.iyzico;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * iyzico payment request DTO
 */
@Data
@Builder
public class IyzicoPaymentRequest {

    // Transaction
    private String conversationId;
    private BigDecimal price;
    private BigDecimal paidPrice;

    // Card (token-based)
    private String cardUserKey;
    private String cardToken;

    // Card (raw - for first-time payment with registration)
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    private boolean registerCard;

    // Buyer info
    private String buyerId;
    private String buyerName;
    private String buyerSurname;
    private String buyerEmail;
    private String buyerIdentityNumber;
    private String buyerAddress;
    private String buyerCity;
    private String buyerIp;

    // Item
    private String itemId;
    private String itemName;
}
