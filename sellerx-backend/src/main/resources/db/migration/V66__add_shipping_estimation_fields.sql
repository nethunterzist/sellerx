-- Add shipping cost estimation fields to support estimated shipping costs
-- Similar to commission estimation system

-- Add shipping cost fields to trendyol_products
ALTER TABLE trendyol_products
    ADD COLUMN IF NOT EXISTS last_shipping_cost_per_unit DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS last_shipping_cost_date TIMESTAMP;

-- Add shipping estimation fields to trendyol_orders
ALTER TABLE trendyol_orders
    ADD COLUMN IF NOT EXISTS estimated_shipping_cost DECIMAL(10, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_shipping_estimated BOOLEAN DEFAULT true;

-- Index for fast lookup of orders needing shipping reconciliation
CREATE INDEX IF NOT EXISTS idx_orders_shipping_estimated
ON trendyol_orders(is_shipping_estimated)
WHERE is_shipping_estimated = true;

-- Index for products with shipping cost data (for estimation lookups)
CREATE INDEX IF NOT EXISTS idx_products_shipping_cost
ON trendyol_products(store_id, barcode)
WHERE last_shipping_cost_per_unit IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN trendyol_products.last_shipping_cost_per_unit IS 'Son kargo faturasından hesaplanan birim kargo maliyeti (TL/adet)';
COMMENT ON COLUMN trendyol_products.last_shipping_cost_date IS 'Bu kargo maliyetinin hesaplandığı tarih';
COMMENT ON COLUMN trendyol_orders.estimated_shipping_cost IS 'Tahmini kargo maliyeti (kargo faturası gelmeden önce)';
COMMENT ON COLUMN trendyol_orders.is_shipping_estimated IS 'true: tahmini değer, false: kargo faturasından gerçek değer';
