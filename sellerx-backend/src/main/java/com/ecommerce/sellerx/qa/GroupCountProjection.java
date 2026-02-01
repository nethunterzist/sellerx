package com.ecommerce.sellerx.qa;

/**
 * Generic projection for group-by-count queries in QA domain.
 */
public interface GroupCountProjection {
    String getGroupName();
    Long getGroupCount();
}
