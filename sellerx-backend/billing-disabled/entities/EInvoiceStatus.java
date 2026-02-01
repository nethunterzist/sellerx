package com.ecommerce.sellerx.billing;

/**
 * E-Invoice (Parasut) status
 */
public enum EInvoiceStatus {
    /** Invoice created locally */
    DRAFT,

    /** Sent to Parasut, waiting for processing */
    PENDING,

    /** E-invoice sent to customer */
    SENT,

    /** E-invoice approved by GIB */
    APPROVED,

    /** E-invoice rejected */
    REJECTED,

    /** E-invoice cancelled */
    CANCELLED
}
