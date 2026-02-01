package com.ecommerce.sellerx.billing;

/**
 * Webhook processing status for idempotency
 */
public enum WebhookProcessingStatus {
    /** Webhook received, not yet processed */
    RECEIVED,

    /** Currently being processed */
    PROCESSING,

    /** Successfully processed */
    COMPLETED,

    /** Processing failed */
    FAILED,

    /** Duplicate webhook (already processed) */
    DUPLICATE
}
