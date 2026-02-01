-- V20: Create webhook events table for idempotency and audit logging
CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    seller_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    order_number VARCHAR(255),
    status VARCHAR(50),
    payload TEXT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- Index for idempotency lookups (fast duplicate detection)
CREATE INDEX idx_webhook_event_id ON webhook_events(event_id);

-- Index for event log queries by store
CREATE INDEX idx_webhook_store_created ON webhook_events(store_id, created_at DESC);

-- Index for filtering by event type
CREATE INDEX idx_webhook_event_type ON webhook_events(event_type);

-- Index for processing status monitoring
CREATE INDEX idx_webhook_processing_status ON webhook_events(processing_status);
