-- Add cancellation information fields to trendyol_orders
-- Trendyol API changelog 08.12.2025: cancelledBy, cancelReason, cancelReasonCode added
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(50);
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
ALTER TABLE trendyol_orders ADD COLUMN IF NOT EXISTS cancel_reason_code VARCHAR(100);
