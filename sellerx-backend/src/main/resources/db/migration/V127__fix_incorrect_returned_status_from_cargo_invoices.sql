-- Fix orders incorrectly marked as 'Returned' by cargo invoice sync (V120).
-- Cargo invoices with 'İade Kargo Bedeli' set status='Returned' on orders
-- that were actually 'Delivered' or other statuses. Restore the correct status
-- from shipment_package_status (the authoritative Trendyol API status).
-- Only fix orders that have return_shipping_cost > 0 (came from cargo invoice update)
-- and whose shipment_package_status is NOT a real return status.

UPDATE trendyol_orders
SET status = shipment_package_status,
    updated_at = NOW()
WHERE status = 'Returned'
  AND shipment_package_status IS NOT NULL
  AND shipment_package_status NOT IN ('UnDeliveredAndReturned')
  AND return_shipping_cost > 0;
