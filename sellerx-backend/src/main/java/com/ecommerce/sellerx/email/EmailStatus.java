package com.ecommerce.sellerx.email;

/**
 * Status of an email in the queue.
 */
public enum EmailStatus {
    PENDING,    // Waiting to be sent
    SENDING,    // Currently being processed
    SENT,       // Successfully sent
    FAILED      // Failed after all retries
}
