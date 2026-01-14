-- Add estimated_commission column to trendyol_orders table
ALTER TABLE trendyol_orders 
ADD COLUMN IF NOT EXISTS estimated_commission DECIMAL(10,2) DEFAULT 0.00;

-- Add comment to explain the column
COMMENT ON COLUMN trendyol_orders.estimated_commission IS 'Total estimated commission for all items in this order. Calculated as sum of (unitPriceOrder - unitPriceDiscount) * commissionRate / 100 for each product';
