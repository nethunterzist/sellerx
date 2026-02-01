package com.ecommerce.sellerx.billing;

/**
 * Invoice status
 */
public enum InvoiceStatus {
    /** Invoice created but not finalized */
    DRAFT,

    /** Invoice sent, waiting for payment */
    PENDING,

    /** Payment received */
    PAID,

    /** Payment failed */
    FAILED,

    /** Payment refunded */
    REFUNDED,

    /** Invoice cancelled/voided */
    VOID
}
