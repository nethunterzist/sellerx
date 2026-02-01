-- V51__create_trendyol_payment_orders_table.sql
-- Create table for Trendyol Payment Orders (Hak Edis) for settlement verification

CREATE TABLE IF NOT EXISTS trendyol_payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    payment_order_id BIGINT NOT NULL,
    payment_date TIMESTAMP,
    total_amount DECIMAL(15,2) NOT NULL,
    expected_amount DECIMAL(15,2),
    discrepancy_amount DECIMAL(15,2),
    discrepancy_status VARCHAR(20) DEFAULT 'PENDING',
    settlement_count INTEGER DEFAULT 0,
    sale_amount DECIMAL(15,2) DEFAULT 0,
    sale_count INTEGER DEFAULT 0,
    return_amount DECIMAL(15,2) DEFAULT 0,
    return_count INTEGER DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    coupon_amount DECIMAL(15,2) DEFAULT 0,
    commission_amount DECIMAL(15,2) DEFAULT 0,
    cargo_amount DECIMAL(15,2) DEFAULT 0,
    stoppage_amount DECIMAL(15,2) DEFAULT 0,
    raw_data JSONB,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Unique constraint on payment order ID per store
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_orders_unique
    ON trendyol_payment_orders(store_id, payment_order_id);

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_payment_orders_store_date
    ON trendyol_payment_orders(store_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_payment_orders_status
    ON trendyol_payment_orders(store_id, discrepancy_status);

-- Add comments
COMMENT ON TABLE trendyol_payment_orders IS 'Trendyol payment orders for Hak Edis Kontrolu (settlement verification)';
COMMENT ON COLUMN trendyol_payment_orders.total_amount IS 'Actual amount received from Trendyol';
COMMENT ON COLUMN trendyol_payment_orders.expected_amount IS 'Calculated expected amount from settlements';
COMMENT ON COLUMN trendyol_payment_orders.discrepancy_amount IS 'Difference: expected - actual';
COMMENT ON COLUMN trendyol_payment_orders.discrepancy_status IS 'MATCHED, UNDERPAID, OVERPAID, or PENDING';
