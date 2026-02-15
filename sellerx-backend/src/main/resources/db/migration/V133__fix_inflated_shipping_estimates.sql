-- Fix inflated estimated shipping costs caused by per-item multiplication.
-- Trendyol charges shipping per PACKAGE, not per item.
-- Recalculate: SUM(perUnit × qty) → MAX(perUnit) for estimated orders.

UPDATE trendyol_orders o
SET estimated_shipping_cost = sub.corrected_cost
FROM (
    SELECT o2.id,
           MAX(COALESCE(p.last_shipping_cost_per_unit, 0)) as corrected_cost
    FROM trendyol_orders o2
    CROSS JOIN LATERAL jsonb_array_elements(o2.order_items) AS item
    LEFT JOIN trendyol_products p
      ON p.barcode = item->>'barcode' AND p.store_id = o2.store_id
    WHERE o2.is_shipping_estimated = true
      AND o2.estimated_shipping_cost > 0
    GROUP BY o2.id
) sub
WHERE o.id = sub.id
  AND sub.corrected_cost > 0
  AND sub.corrected_cost < o.estimated_shipping_cost;
