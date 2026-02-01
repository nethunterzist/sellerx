-- V45: Komisyon takip alanları ve performans index'leri
-- Bu migration komisyon hesaplama sistemini destekler:
-- - last_commission_rate: Financial API'den gelen son gerçek komisyon oranı
-- - last_commission_date: Bu oranın geldiği tarih
-- - is_commission_estimated: Tahmini mi (true) yoksa Financial API'den mi (false)

-- Komisyon takip alanları - trendyol_products
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS last_commission_rate DECIMAL(5,2);
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS last_commission_date TIMESTAMP;

-- Komisyon durumu - trendyol_orders
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS is_commission_estimated BOOLEAN DEFAULT TRUE;

-- Performans index'leri
CREATE INDEX IF NOT EXISTS idx_orders_commission_estimated ON trendyol_orders(is_commission_estimated);
CREATE INDEX IF NOT EXISTS idx_products_store_barcode ON trendyol_products(store_id, barcode);
