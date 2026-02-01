package com.ecommerce.sellerx.billing;

/**
 * E-invoice status for Paraşüt integration
 */
public enum EInvoiceStatus {
    /** E-invoice created but not sent */
    DRAFT,

    /** E-invoice pending submission to GİB */
    PENDING,

    /** E-invoice sent to GİB */
    SENT,

    /** E-invoice approved by GİB */
    APPROVED,

    /** E-invoice rejected by GİB */
    REJECTED,

    /** E-invoice cancelled */
    CANCELLED
}
