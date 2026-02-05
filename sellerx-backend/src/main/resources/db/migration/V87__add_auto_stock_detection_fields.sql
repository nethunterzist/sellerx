-- Add previous_trendyol_quantity column to track stock changes for auto-detection
ALTER TABLE trendyol_products ADD COLUMN IF NOT EXISTS previous_trendyol_quantity INTEGER DEFAULT 0;

-- Initialize with current quantity for existing products
UPDATE trendyol_products SET previous_trendyol_quantity = COALESCE(trendyol_quantity, 0);
