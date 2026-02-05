-- V85: Add delivery date and hakediş date fields for settlement verification
-- delivery_date: When the order was actually delivered (from Trendyol webhook packageHistories)
-- hakedis_date: Calculated settlement date (delivery_date + 28 days, snapped to next Monday or Thursday)

ALTER TABLE trendyol_orders ADD COLUMN delivery_date TIMESTAMP;
ALTER TABLE trendyol_orders ADD COLUMN hakedis_date DATE;

-- Partial indexes for efficient hakediş queries (only non-null values)
CREATE INDEX idx_orders_store_hakedis ON trendyol_orders(store_id, hakedis_date) WHERE hakedis_date IS NOT NULL;
CREATE INDEX idx_orders_store_delivery ON trendyol_orders(store_id, delivery_date) WHERE delivery_date IS NOT NULL;

-- Backfill: For delivered orders, use updated_at as approximate delivery date
-- This will be refined later when we recalculate from actual Trendyol data
UPDATE trendyol_orders
SET delivery_date = updated_at
WHERE status = 'Delivered' AND delivery_date IS NULL;
