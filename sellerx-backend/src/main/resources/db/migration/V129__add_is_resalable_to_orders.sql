-- Add is_resalable column to track whether returned products can be resold
-- NULL = decision pending (default: only shipping costs counted as loss)
-- TRUE = resalable (only shipping costs as loss)
-- FALSE = not resalable (shipping + product cost as loss)

ALTER TABLE trendyol_orders ADD COLUMN is_resalable BOOLEAN DEFAULT NULL;
ALTER TABLE return_records ADD COLUMN is_resalable BOOLEAN DEFAULT NULL;
