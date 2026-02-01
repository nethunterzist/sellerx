-- Add coupon discount and early payment fee columns to trendyol_orders
-- These fields store financial data from Trendyol Settlement API

ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS coupon_discount DECIMAL(10,2) DEFAULT 0;
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS early_payment_fee DECIMAL(10,2) DEFAULT 0;

-- Add indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_orders_coupon_discount ON trendyol_orders(coupon_discount) WHERE coupon_discount > 0;
CREATE INDEX IF NOT EXISTS idx_orders_early_payment_fee ON trendyol_orders(early_payment_fee) WHERE early_payment_fee > 0;
