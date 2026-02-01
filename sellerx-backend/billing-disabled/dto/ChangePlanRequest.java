package com.ecommerce.sellerx.billing.dto;

import com.ecommerce.sellerx.billing.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to change subscription plan
 */
@Data
public class ChangePlanRequest {

    @NotBlank(message = "Plan code is required")
    private String planCode;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;
}
