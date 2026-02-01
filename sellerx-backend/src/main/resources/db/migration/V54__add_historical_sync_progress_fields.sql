-- Add progress tracking fields for historical sync
ALTER TABLE stores 
ADD COLUMN IF NOT EXISTS historical_sync_checkpoint_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS historical_sync_start_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS historical_sync_total_chunks INTEGER,
ADD COLUMN IF NOT EXISTS historical_sync_completed_chunks INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS historical_sync_current_processing_date TIMESTAMP;

-- Create table for tracking failed chunks (poisonous chunks)
CREATE TABLE IF NOT EXISTS historical_sync_failed_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    chunk_start_date TIMESTAMP NOT NULL,
    chunk_end_date TIMESTAMP NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error_message TEXT,
    failed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_failed_chunk_store FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX IF NOT EXISTS idx_failed_chunks_store ON historical_sync_failed_chunks(store_id);
CREATE INDEX IF NOT EXISTS idx_failed_chunks_retry ON historical_sync_failed_chunks(store_id, retry_count);

COMMENT ON TABLE historical_sync_failed_chunks IS 'Tracks failed chunks during historical sync to prevent infinite retry loops';
COMMENT ON COLUMN historical_sync_failed_chunks.retry_count IS 'Number of times this chunk has failed. If >= 5, chunk is marked as permanently failed.';
COMMENT ON COLUMN stores.historical_sync_checkpoint_date IS 'Last successfully processed chunk date. Used for resumable sync.';
COMMENT ON COLUMN stores.historical_sync_current_processing_date IS 'Current date being processed. Used for frontend progress display.';
