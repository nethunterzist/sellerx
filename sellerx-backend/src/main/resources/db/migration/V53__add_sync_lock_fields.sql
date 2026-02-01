-- Add sync lock fields to prevent concurrent sync operations for the same store
ALTER TABLE stores 
ADD COLUMN IF NOT EXISTS sync_lock_acquired_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS sync_lock_thread_id VARCHAR(255);

-- Index for efficient lock cleanup queries (finding stale locks)
CREATE INDEX IF NOT EXISTS idx_stores_sync_lock ON stores(sync_lock_acquired_at);

-- Comment explaining the lock mechanism
COMMENT ON COLUMN stores.sync_lock_acquired_at IS 'Timestamp when sync lock was acquired. Null means no lock. Locks older than 2 hours are considered stale.';
COMMENT ON COLUMN stores.sync_lock_thread_id IS 'Thread ID that acquired the sync lock. Used for debugging and identifying which thread holds the lock.';
