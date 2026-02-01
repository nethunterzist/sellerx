-- Add B-tree index on webhook_events.seller_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_webhook_events_seller_id ON webhook_events(seller_id);
