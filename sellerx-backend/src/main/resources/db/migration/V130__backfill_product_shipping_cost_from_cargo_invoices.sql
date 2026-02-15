-- V130: Backfill last_shipping_cost_per_unit from cargo invoices via order join
--
-- Problem: Trendyol's cargo invoice API never returns barcode field.
-- All 24,590 cargo invoice records have empty barcode in raw_data.
-- This means 264 products have NULL last_shipping_cost_per_unit.
--
-- Solution: Join cargo invoices to orders via order_number, extract barcode
-- from order_items JSONB. Only use single-item orders for accuracy
-- (multi-item orders have combined shipping cost that can't be split per product).

WITH latest_shipping AS (
    SELECT DISTINCT ON (item->>'barcode', ci.store_id)
           item->>'barcode' AS barcode,
           ci.amount AS shipping_cost,
           ci.store_id
    FROM trendyol_cargo_invoices ci
    JOIN trendyol_orders o ON ci.order_number = o.ty_order_number AND ci.store_id = o.store_id
    CROSS JOIN LATERAL jsonb_array_elements(o.order_items) AS item
    WHERE ci.shipment_package_type = 'Gönderi Kargo Bedeli'
      AND jsonb_array_length(o.order_items) = 1
      AND item->>'barcode' IS NOT NULL AND item->>'barcode' != ''
      AND ci.amount > 0
    ORDER BY item->>'barcode', ci.store_id, ci.invoice_date DESC
)
UPDATE trendyol_products p
SET last_shipping_cost_per_unit = ls.shipping_cost,
    last_shipping_cost_date = NOW()
FROM latest_shipping ls
WHERE p.barcode = ls.barcode
  AND p.store_id = ls.store_id
  AND (p.last_shipping_cost_per_unit IS NULL OR p.last_shipping_cost_per_unit = 0);
