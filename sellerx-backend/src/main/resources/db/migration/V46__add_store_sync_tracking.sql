-- V46: Store sync ve webhook tracking alanları
-- Bu migration mağaza senkronizasyon durumunu takip eder:
-- - webhook_status: pending, active, failed, inactive
-- - sync_status: pending, SYNCING_PRODUCTS, SYNCING_ORDERS, SYNCING_FINANCIAL, COMPLETED, FAILED

-- Webhook tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS webhook_error_message TEXT;

-- Sync tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_status VARCHAR(50) DEFAULT 'pending';
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_error_message TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS initial_sync_completed BOOLEAN DEFAULT FALSE;
