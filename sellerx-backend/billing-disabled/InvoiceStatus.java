package com.ecommerce.sellerx.billing;

/**
 * Invoice status lifecycle
 */
public enum InvoiceStatus {
    /** Invoice created but not yet finalized */
    DRAFT,

    /** Invoice finalized, awaiting payment */
    PENDING,

    /** Payment successful */
    PAID,

    /** Payment failed */
    FAILED,

    /** Payment refunded */
    REFUNDED,

    /** Invoice voided/cancelled */
    VOID
}
