-- Stopaj artık her zaman %1 hesaplanan değer kullanılıyor.
-- API reconciliation (tahmini → gerçek) mantığı kaldırılıyor.
ALTER TABLE trendyol_orders DROP COLUMN IF EXISTS is_stoppage_estimated;
ALTER TABLE trendyol_orders DROP COLUMN IF EXISTS stoppage_difference;
