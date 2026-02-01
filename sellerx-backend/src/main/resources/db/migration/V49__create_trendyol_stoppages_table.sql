-- V49__create_trendyol_stoppages_table.sql
-- Create table for Trendyol Stoppage (Tevkifat) transactions

CREATE TABLE IF NOT EXISTS trendyol_stoppages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    transaction_id VARCHAR(100),
    transaction_date TIMESTAMP NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    invoice_serial_number VARCHAR(100),
    payment_order_id BIGINT,
    receipt_id BIGINT,
    description TEXT,
    period_start DATE,
    period_end DATE,
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Unique constraint to prevent duplicates
CREATE UNIQUE INDEX IF NOT EXISTS idx_stoppages_unique
    ON trendyol_stoppages(store_id, transaction_date, amount, COALESCE(transaction_id, ''));

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_stoppages_store_date
    ON trendyol_stoppages(store_id, transaction_date);

CREATE INDEX IF NOT EXISTS idx_stoppages_payment_order
    ON trendyol_stoppages(store_id, payment_order_id);

-- Add comments
COMMENT ON TABLE trendyol_stoppages IS 'Trendyol Stoppage (Tevkifat/Withholding Tax) transactions from otherfinancials API';
COMMENT ON COLUMN trendyol_stoppages.amount IS 'Stoppage amount deducted by Trendyol';
COMMENT ON COLUMN trendyol_stoppages.payment_order_id IS 'Payment order ID for grouping with settlements';
