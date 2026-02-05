-- Add status field to alert_history for approval-based stock detection
ALTER TABLE alert_history ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'INFO';

-- Existing alerts remain as INFO (informational)
-- New stock detection alerts will be created with PENDING_APPROVAL status
