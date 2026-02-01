package com.ecommerce.sellerx.billing.dto;

import com.ecommerce.sellerx.billing.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to create a new subscription
 */
@Data
public class CreateSubscriptionRequest {

    @NotBlank(message = "Plan code is required")
    private String planCode;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    // Card details for trial with card requirement
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;

    // Or use existing card
    private String cardToken;
}
