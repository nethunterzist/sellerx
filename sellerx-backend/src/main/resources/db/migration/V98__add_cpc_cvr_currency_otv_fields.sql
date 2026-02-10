-- V98: Add CPC, CVR, Currency, and OTV fields for profit calculator
-- Based on Excel "seller x için calculator.xls" analysis

-- Add advertising metrics columns (Excel C23, C24)
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS cpc DECIMAL(10, 2);
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS cvr DECIMAL(5, 4);

-- Add default currency columns (Excel F1)
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS default_currency VARCHAR(3);
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS default_exchange_rate DECIMAL(10, 4);

-- Add OTV (Special Consumption Tax) column (Excel F5)
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS otv_rate DECIMAL(5, 4);

-- Add comments for documentation
COMMENT ON COLUMN trendyol_products.cpc IS 'Cost Per Click (TL) - Excel C23';
COMMENT ON COLUMN trendyol_products.cvr IS 'Conversion Rate (e.g., 0.018 = 1.8%) - Excel C24';
COMMENT ON COLUMN trendyol_products.default_currency IS 'Default currency for cost: TRY, USD, EUR';
COMMENT ON COLUMN trendyol_products.default_exchange_rate IS 'Default exchange rate for foreign currency - Excel F1';
COMMENT ON COLUMN trendyol_products.otv_rate IS 'Special Consumption Tax rate (e.g., 0.20 = 20%) - Excel F5';

-- Note: costAndStockInfo JSONB already supports per-stock-entry values for:
-- - currency, exchangeRate, foreignCost (per stock entry)
-- - otvRate (per stock entry)
-- - cpc, cvr (per stock entry - for historical tracking)
-- The product-level columns above serve as defaults for new stock entries
