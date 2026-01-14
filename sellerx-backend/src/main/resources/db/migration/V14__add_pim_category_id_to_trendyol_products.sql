-- Add pim_category_id column to trendyol_products table
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS pim_category_id BIGINT;

-- Add comment to explain the column
COMMENT ON COLUMN trendyol_products.pim_category_id IS 'PIM Category ID from Trendyol API';
