-- Add return_shipping_cost column to track return cargo costs separately
ALTER TABLE trendyol_orders
    ADD COLUMN IF NOT EXISTS return_shipping_cost NUMERIC(10, 2) DEFAULT 0;

-- Populate return_shipping_cost from existing return cargo invoices
-- Uses SUM because an order could have multiple return cargo invoice entries
UPDATE trendyol_orders o
SET return_shipping_cost = sub.total_return_cost
FROM (
    SELECT c.order_number, c.store_id, SUM(c.amount) as total_return_cost
    FROM trendyol_cargo_invoices c
    WHERE c.shipment_package_type = 'İade Kargo Bedeli'
    GROUP BY c.order_number, c.store_id
) sub
WHERE o.ty_order_number = sub.order_number
  AND o.store_id = sub.store_id;

-- Fix estimated_shipping_cost for orders that were matched with return cargo invoices
-- (the old query didn't filter by type, so some orders may have return cost as shipping cost)
-- Re-match with only 'Gönderi Kargo Bedeli' type
UPDATE trendyol_orders o
SET estimated_shipping_cost = sub.total_send_cost,
    is_shipping_estimated = false
FROM (
    SELECT c.order_number, c.store_id, SUM(c.amount) as total_send_cost
    FROM trendyol_cargo_invoices c
    WHERE c.shipment_package_type = 'Gönderi Kargo Bedeli'
    GROUP BY c.order_number, c.store_id
) sub
WHERE o.ty_order_number = sub.order_number
  AND o.store_id = sub.store_id
  AND o.is_shipping_estimated = false;
