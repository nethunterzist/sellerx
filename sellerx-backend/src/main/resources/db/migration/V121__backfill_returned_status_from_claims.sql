-- V121: Backfill order status to 'Returned' from existing accepted claims.
-- This bridges the gap where claims were synced but order status was never updated.
-- Only updates orders that are not already 'Returned' or 'Cancelled'.
-- Uses trendyol_claims.order_number ↔ trendyol_orders.ty_order_number mapping.

UPDATE trendyol_orders o
SET status = 'Returned'
FROM (
    SELECT DISTINCT c.order_number, c.store_id
    FROM trendyol_claims c
    WHERE c.status = 'Accepted'
      AND c.order_number IS NOT NULL
) sub
WHERE o.ty_order_number = sub.order_number
  AND o.store_id = sub.store_id
  AND o.status NOT IN ('Returned', 'Cancelled');
