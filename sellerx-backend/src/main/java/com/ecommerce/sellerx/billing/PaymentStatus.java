package com.ecommerce.sellerx.billing;

/**
 * Payment transaction status
 */
public enum PaymentStatus {
    /** Payment initiated */
    PENDING,

    /** Payment is being processed */
    PROCESSING,

    /** Payment successful */
    SUCCESS,

    /** Payment failed */
    FAILED,

    /** Payment refunded */
    REFUNDED
}
