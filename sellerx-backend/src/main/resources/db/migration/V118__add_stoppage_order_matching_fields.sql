-- V118: Add stoppage order matching fields
-- Following the same pattern as commission (isCommissionEstimated) and cargo (orderNumber, shipmentPackageId)

-- Add order matching fields to trendyol_stoppages table
ALTER TABLE trendyol_stoppages ADD COLUMN IF NOT EXISTS order_number VARCHAR(50);
ALTER TABLE trendyol_stoppages ADD COLUMN IF NOT EXISTS shipment_package_id BIGINT;

-- Create indexes for efficient order-stoppage matching
CREATE INDEX IF NOT EXISTS idx_stoppage_store_order ON trendyol_stoppages(store_id, order_number);
CREATE INDEX IF NOT EXISTS idx_stoppage_store_package ON trendyol_stoppages(store_id, shipment_package_id);

-- Add stoppage estimation tracking fields to trendyol_orders table
-- Similar to isCommissionEstimated and commissionDifference
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS is_stoppage_estimated BOOLEAN DEFAULT true;
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS stoppage_difference DECIMAL(15,2);

-- Comment explaining the fields
COMMENT ON COLUMN trendyol_stoppages.order_number IS 'Trendyol order number for matching stoppage to order';
COMMENT ON COLUMN trendyol_stoppages.shipment_package_id IS 'Trendyol shipment package ID for matching stoppage to order';
COMMENT ON COLUMN trendyol_orders.is_stoppage_estimated IS 'true: estimated from order, false: real value from Financial API';
COMMENT ON COLUMN trendyol_orders.stoppage_difference IS 'Difference between real and estimated stoppage (real - estimated)';
