package com.ecommerce.sellerx.billing;

/**
 * Generic projection for type/count grouping queries.
 */
public interface TypeCountProjection {
    String getTypeName();
    Long getTypeCount();
}
