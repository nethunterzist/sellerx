-- Add settlement-related columns to trendyol_orders table
-- Only add columns if they don't exist

DO $$ 
BEGIN 
    -- Add transaction_date column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'trendyol_orders' 
                   AND column_name = 'transaction_date') THEN
        ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP;
    END IF;
    
    -- Add transaction_status column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'trendyol_orders' 
                   AND column_name = 'transaction_status') THEN
        ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS transaction_status VARCHAR(50) DEFAULT 'NOT_SETTLED';
    END IF;
END $$;

-- Add indexes only if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'trendyol_orders' 
                   AND indexname = 'idx_trendyol_orders_transaction_status') THEN
        CREATE INDEX idx_trendyol_orders_transaction_status ON trendyol_orders(transaction_status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'trendyol_orders' 
                   AND indexname = 'idx_trendyol_orders_transaction_date') THEN
        CREATE INDEX idx_trendyol_orders_transaction_date ON trendyol_orders(transaction_date);
    END IF;
END $$;
