-- Backfill: İade kargo faturası olan ama status='Returned' olmayan siparişleri düzelt.
-- Trendyol Orders API "Returned" statüsü dönmüyor; ancak kargo faturasındaki
-- 'İade Kargo Bedeli' kaydı, siparişin iade edildiğinin kesin kanıtıdır.
UPDATE trendyol_orders o
SET status = 'Returned'
FROM (
    SELECT DISTINCT c.order_number, c.store_id
    FROM trendyol_cargo_invoices c
    WHERE c.shipment_package_type = 'İade Kargo Bedeli'
) sub
WHERE o.ty_order_number = sub.order_number
  AND o.store_id = sub.store_id
  AND o.status != 'Returned';
