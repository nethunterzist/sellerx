-- Add commission_source column to track where commission data comes from
-- INVOICE: From Financial/Settlement API (lastCommissionRate)
-- REFERENCE: From Product API (commissionRate)
-- NONE: No commission data available
-- MANUAL: Manually entered (legacy)

ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS commission_source VARCHAR(20);

COMMENT ON COLUMN trendyol_orders.commission_source IS 'Komisyon veri kaynağı: INVOICE (faturadan), REFERENCE (ürün referansından), NONE (veri yok), MANUAL (manuel giriş)';

-- Index for filtering by commission source
CREATE INDEX IF NOT EXISTS idx_trendyol_orders_commission_source ON trendyol_orders(commission_source);
