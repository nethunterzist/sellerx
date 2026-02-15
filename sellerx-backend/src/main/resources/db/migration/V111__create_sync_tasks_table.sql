-- ================================================
-- V111: Create sync_tasks table for async sync tracking
-- ================================================
-- Enables non-blocking sync operations with progress tracking
-- Users can start sync and poll for status instead of waiting

CREATE TABLE sync_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    task_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress_percentage INTEGER DEFAULT 0,
    current_page INTEGER,
    total_pages INTEGER,
    items_processed INTEGER DEFAULT 0,
    items_new INTEGER DEFAULT 0,
    items_updated INTEGER DEFAULT 0,
    items_skipped INTEGER DEFAULT 0,
    items_failed INTEGER DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT chk_task_type CHECK (task_type IN ('PRODUCTS', 'ORDERS', 'FINANCIAL', 'RETURNS', 'ALL')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Index for finding tasks by store and type (common query pattern)
CREATE INDEX idx_sync_task_store_type ON sync_tasks(store_id, task_type);

-- Index for filtering by status (for finding active tasks)
CREATE INDEX idx_sync_task_status ON sync_tasks(status);

-- Index for cleanup job (finding old completed tasks)
CREATE INDEX idx_sync_task_created ON sync_tasks(created_at);

-- Composite index for finding active tasks of a specific type
CREATE INDEX idx_sync_task_store_type_status ON sync_tasks(store_id, task_type, status)
    WHERE status IN ('PENDING', 'RUNNING');

COMMENT ON TABLE sync_tasks IS 'Tracks async sync operations (products, orders, financial) with progress';
COMMENT ON COLUMN sync_tasks.task_type IS 'Type: PRODUCTS, ORDERS, FINANCIAL, RETURNS, ALL';
COMMENT ON COLUMN sync_tasks.status IS 'Status: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN sync_tasks.progress_percentage IS 'Progress 0-100%';
