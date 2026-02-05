-- Drop hakedis-related columns and indexes
DROP INDEX IF EXISTS idx_orders_store_hakedis;
ALTER TABLE trendyol_orders DROP COLUMN IF EXISTS hakedis_date;
