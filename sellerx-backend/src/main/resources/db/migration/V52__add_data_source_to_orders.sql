-- V52: Add data_source column to track where order data came from
-- This enables storing historical orders from Settlements API

-- Add data_source column to trendyol_orders
ALTER TABLE trendyol_orders
ADD COLUMN data_source VARCHAR(50) DEFAULT 'ORDER_API';

-- Update existing orders (all were from Orders API)
UPDATE trendyol_orders SET data_source = 'ORDER_API' WHERE data_source IS NULL;

-- Add NOT NULL constraint after backfill
ALTER TABLE trendyol_orders ALTER COLUMN data_source SET NOT NULL;

-- Index for filtering by data source
CREATE INDEX idx_trendyol_orders_data_source ON trendyol_orders(data_source);

-- Add historical sync tracking to stores
ALTER TABLE stores ADD COLUMN historical_sync_status VARCHAR(50);
ALTER TABLE stores ADD COLUMN historical_sync_date TIMESTAMP;

-- Comments for documentation
COMMENT ON COLUMN trendyol_orders.data_source IS
'Source of order data: ORDER_API (full data from Trendyol Orders API for last 90 days) or SETTLEMENT_API (limited data from Financial Settlements API for historical orders >90 days)';

COMMENT ON COLUMN stores.historical_sync_status IS
'Status of historical settlement sync: null (not started), COMPLETED, PARTIAL (some data missing), FAILED, SKIPPED (store < 90 days old)';

COMMENT ON COLUMN stores.historical_sync_date IS
'Timestamp when historical settlement sync was last completed';
