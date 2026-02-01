package com.ecommerce.sellerx.stores;

/**
 * Overall status of parallel sync execution for a store.
 */
public enum OverallSyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    PARTIAL_COMPLETE
}
