-- Add shipment city/district columns to trendyol_orders table for city-based analytics
ALTER TABLE trendyol_orders
ADD COLUMN shipment_city VARCHAR(100),
ADD COLUMN shipment_city_code INTEGER,
ADD COLUMN shipment_district VARCHAR(100),
ADD COLUMN shipment_district_id INTEGER;

-- Add indexes for city-based queries
CREATE INDEX idx_orders_shipment_city ON trendyol_orders(shipment_city);
CREATE INDEX idx_orders_shipment_city_code ON trendyol_orders(shipment_city_code);

-- Add comment for documentation
COMMENT ON COLUMN trendyol_orders.shipment_city IS 'City name from Trendyol shipment address (e.g., Istanbul)';
COMMENT ON COLUMN trendyol_orders.shipment_city_code IS 'City plate code from Trendyol (e.g., 34 for Istanbul)';
COMMENT ON COLUMN trendyol_orders.shipment_district IS 'District name from Trendyol shipment address (e.g., Kadikoy)';
COMMENT ON COLUMN trendyol_orders.shipment_district_id IS 'District ID from Trendyol';
