package com.ecommerce.sellerx.billing.dto;

import lombok.Data;

/**
 * Request to cancel subscription
 */
@Data
public class CancelSubscriptionRequest {

    private String reason;
}
