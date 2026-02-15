-- V112: Data Retention Policies
-- Target: Prevent unbounded table growth for high-volume tables
-- Policy: webhook_events 90 days, email_queue SENT 30 days, activity_logs 1 year

-- ============================================
-- 1. RETENTION FUNCTION: Clean old webhook events
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_webhook_events(retention_days INTEGER DEFAULT 90)
RETURNS TABLE(deleted_count BIGINT) AS $$
DECLARE
    rows_deleted BIGINT;
BEGIN
    DELETE FROM webhook_events
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN QUERY SELECT rows_deleted;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_webhook_events IS
    'Deletes webhook_events older than specified days (default 90). Returns number of deleted rows.';

-- ============================================
-- 2. RETENTION FUNCTION: Clean sent emails
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_sent_emails(retention_days INTEGER DEFAULT 30)
RETURNS TABLE(deleted_count BIGINT) AS $$
DECLARE
    rows_deleted BIGINT;
BEGIN
    DELETE FROM email_queue
    WHERE status = 'SENT'
      AND sent_at < NOW() - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN QUERY SELECT rows_deleted;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_sent_emails IS
    'Deletes successfully sent emails older than specified days (default 30). Returns number of deleted rows.';

-- ============================================
-- 3. RETENTION FUNCTION: Clean failed emails
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_failed_emails(retention_days INTEGER DEFAULT 90)
RETURNS TABLE(deleted_count BIGINT) AS $$
DECLARE
    rows_deleted BIGINT;
BEGIN
    DELETE FROM email_queue
    WHERE status = 'FAILED'
      AND updated_at < NOW() - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN QUERY SELECT rows_deleted;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_failed_emails IS
    'Deletes failed emails older than specified days (default 90). Returns number of deleted rows.';

-- ============================================
-- 4. RETENTION FUNCTION: Clean activity logs
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_activity_logs(retention_days INTEGER DEFAULT 365)
RETURNS TABLE(deleted_count BIGINT) AS $$
DECLARE
    rows_deleted BIGINT;
BEGIN
    DELETE FROM activity_logs
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN QUERY SELECT rows_deleted;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_activity_logs IS
    'Deletes activity_logs older than specified days (default 365). Returns number of deleted rows.';

-- ============================================
-- 5. RETENTION FUNCTION: Clean sync tasks
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_old_sync_tasks(retention_days INTEGER DEFAULT 30)
RETURNS TABLE(deleted_count BIGINT) AS $$
DECLARE
    rows_deleted BIGINT;
BEGIN
    DELETE FROM sync_tasks
    WHERE status IN ('COMPLETED', 'FAILED')
      AND updated_at < NOW() - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN QUERY SELECT rows_deleted;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_sync_tasks IS
    'Deletes completed/failed sync_tasks older than specified days (default 30). Returns number of deleted rows.';

-- ============================================
-- 6. MASTER CLEANUP PROCEDURE
-- ============================================
CREATE OR REPLACE PROCEDURE run_all_retention_cleanups()
LANGUAGE plpgsql AS $$
DECLARE
    webhook_deleted BIGINT;
    sent_email_deleted BIGINT;
    failed_email_deleted BIGINT;
    activity_deleted BIGINT;
    sync_deleted BIGINT;
BEGIN
    -- Run all cleanup functions
    SELECT deleted_count INTO webhook_deleted FROM cleanup_old_webhook_events(90);
    SELECT deleted_count INTO sent_email_deleted FROM cleanup_old_sent_emails(30);
    SELECT deleted_count INTO failed_email_deleted FROM cleanup_old_failed_emails(90);
    SELECT deleted_count INTO activity_deleted FROM cleanup_old_activity_logs(365);
    SELECT deleted_count INTO sync_deleted FROM cleanup_old_sync_tasks(30);

    -- Log results
    RAISE NOTICE 'Retention cleanup completed: webhook_events=%, sent_emails=%, failed_emails=%, activity_logs=%, sync_tasks=%',
        webhook_deleted, sent_email_deleted, failed_email_deleted, activity_deleted, sync_deleted;
END;
$$;

COMMENT ON PROCEDURE run_all_retention_cleanups IS
    'Runs all retention cleanup functions. Call this nightly via pg_cron or application scheduler.';

-- ============================================
-- 7. INDEX FOR WEBHOOK EVENTS LOOKUPS (OPTIMIZATION)
-- ============================================
-- Create index for webhook events lookups by store and date
-- Note: Partial indexes with CURRENT_DATE not supported (not IMMUTABLE)
CREATE INDEX IF NOT EXISTS idx_webhook_events_store_date
    ON webhook_events(store_id, created_at DESC);

COMMENT ON INDEX idx_webhook_events_store_date IS
    'Index for webhook events by store and date. Optimizes recent event lookups.';

-- ============================================
-- 8. PARTIAL INDEX FOR PENDING EMAILS
-- ============================================
CREATE INDEX IF NOT EXISTS idx_email_queue_pending_priority
    ON email_queue(scheduled_at, created_at)
    WHERE status = 'PENDING';

COMMENT ON INDEX idx_email_queue_pending_priority IS
    'Partial index for pending emails. Optimizes queue processing queries.';
