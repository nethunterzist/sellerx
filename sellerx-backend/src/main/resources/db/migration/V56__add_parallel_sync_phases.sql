-- V56: Add parallel sync phases support
-- Enables tracking multiple sync phases running in parallel

-- Add sync_phases JSONB column for detailed phase tracking
-- Structure: { "PRODUCTS": { "status": "COMPLETED", "startedAt": "...", "completedAt": "..." }, ... }
ALTER TABLE stores ADD COLUMN IF NOT EXISTS sync_phases JSONB DEFAULT '{}';

-- Add overall_sync_status for high-level status tracking
ALTER TABLE stores ADD COLUMN IF NOT EXISTS overall_sync_status VARCHAR(50);

-- Create index for querying stores by overall status
CREATE INDEX IF NOT EXISTS idx_stores_overall_sync_status ON stores(overall_sync_status);

-- Add comment for documentation
COMMENT ON COLUMN stores.sync_phases IS 'JSON object tracking individual sync phase statuses for parallel execution. Keys: PRODUCTS, HISTORICAL, FINANCIAL, GAP, COMMISSIONS, RETURNS, QA';
COMMENT ON COLUMN stores.overall_sync_status IS 'High-level sync status: PENDING, IN_PROGRESS, COMPLETED, FAILED';
