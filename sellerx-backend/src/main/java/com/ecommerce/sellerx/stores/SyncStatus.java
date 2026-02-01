package com.ecommerce.sellerx.stores;

/**
 * Legacy sync status for store onboarding.
 * Tracks which phase of the initial data sync is currently running.
 */
public enum SyncStatus {
    PENDING,
    SYNCING_PRODUCTS,
    SYNCING_ORDERS,
    SYNCING_FINANCIAL,
    SYNCING_HISTORICAL,
    SYNCING_GAP,
    SYNCING_RETURNS,
    SYNCING_QA,
    RECALCULATING_COMMISSIONS,
    COMPLETED,
    FAILED,
    CANCELLED,
    PARTIAL_COMPLETE
}
