package com.ecommerce.sellerx.stores;

/**
 * Status of a single sync phase within the parallel sync process.
 * Used inside PhaseStatus JSONB objects.
 */
public enum PhaseStatusType {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED
}
