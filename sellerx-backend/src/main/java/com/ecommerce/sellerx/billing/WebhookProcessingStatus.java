package com.ecommerce.sellerx.billing;

/**
 * Webhook event processing status for idempotency
 */
public enum WebhookProcessingStatus {
    /** Webhook received but not yet processed */
    RECEIVED,

    /** Webhook is being processed */
    PROCESSING,

    /** Webhook processed successfully */
    COMPLETED,

    /** Webhook processing failed */
    FAILED,

    /** Duplicate webhook (already processed) */
    DUPLICATE
}
