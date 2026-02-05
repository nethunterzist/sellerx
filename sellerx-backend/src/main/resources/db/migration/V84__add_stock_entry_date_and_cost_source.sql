-- V84: Add stockEntryDate for FIFO lot tracking and stockDepleted flag for warnings
-- stockEntryDate represents when items physically entered inventory (separate from poDate)

-- 1. Add stockEntryDate to purchase_orders table
ALTER TABLE purchase_orders ADD COLUMN stock_entry_date DATE;

-- 2. Add stockEntryDate override to purchase_order_items table (optional per-item override)
ALTER TABLE purchase_order_items ADD COLUMN stock_entry_date DATE;

-- 3. Add stockDepleted flag to trendyol_products table
ALTER TABLE trendyol_products ADD COLUMN stock_depleted BOOLEAN DEFAULT FALSE;

-- 4. Backfill: set stockEntryDate = poDate for existing CLOSED POs
UPDATE purchase_orders SET stock_entry_date = po_date WHERE status = 'CLOSED';
