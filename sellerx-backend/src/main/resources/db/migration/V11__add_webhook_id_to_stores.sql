-- V11: Add webhook_id column to stores table
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_id VARCHAR(255);

-- Add index for webhook_id lookups
CREATE INDEX idx_stores_webhook_id ON stores(webhook_id);
