-- Add stoppage column to trendyol_orders table
ALTER TABLE trendyol_orders 
ADD COLUMN IF NOT EXISTS stoppage DECIMAL(10,2) DEFAULT 0.00;

-- Add comment for documentation
COMMENT ON COLUMN trendyol_orders.stoppage IS 'Stoppage (withholding tax) amount calculated as total_price * stoppage_rate';
