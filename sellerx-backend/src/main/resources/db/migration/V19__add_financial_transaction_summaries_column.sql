-- Add financial_transactions column to separate financial data from order items
-- This column will store both transactions and their summaries
ALTER TABLE trendyol_orders 
ADD COLUMN financial_transactions JSONB;

-- Create index on the new column for better query performance
CREATE INDEX idx_trendyol_orders_financial_transactions ON trendyol_orders USING GIN (financial_transactions);

-- Add comment explaining the purpose of this column
COMMENT ON COLUMN trendyol_orders.financial_transactions IS 'Financial transactions and summaries separated from order items for better data organization';
