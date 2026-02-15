-- V114: Auto-vacuum Tuning for High-Traffic Tables
-- Target: Optimize vacuum/analyze for tables with heavy write activity
-- These settings ensure statistics stay up-to-date for query planner

-- ============================================
-- HIGH-TRAFFIC TABLES: More aggressive vacuum
-- ============================================

-- trendyol_orders: Heavy inserts from sync, 225M+ rows/year at scale
-- Lower scale factor = more frequent vacuum on smaller changes
ALTER TABLE trendyol_orders SET (
    autovacuum_vacuum_scale_factor = 0.05,      -- Vacuum after 5% dead rows (vs default 20%)
    autovacuum_analyze_scale_factor = 0.02,     -- Analyze after 2% changes (vs default 10%)
    autovacuum_vacuum_threshold = 1000,         -- Minimum rows before vacuum
    autovacuum_analyze_threshold = 500          -- Minimum rows before analyze
);

-- webhook_events: Constant inserts from webhooks
ALTER TABLE webhook_events SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 500,
    autovacuum_analyze_threshold = 250
);

-- email_queue: Frequent inserts/updates/deletes
ALTER TABLE email_queue SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 200,
    autovacuum_analyze_threshold = 100
);

-- activity_logs: Constant inserts from user activity
ALTER TABLE activity_logs SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 500,
    autovacuum_analyze_threshold = 250
);

-- sync_tasks: Frequent status updates
ALTER TABLE sync_tasks SET (
    autovacuum_vacuum_scale_factor = 0.10,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 100,
    autovacuum_analyze_threshold = 50
);

-- ============================================
-- MEDIUM-TRAFFIC TABLES: Balanced settings
-- ============================================

-- trendyol_products: Periodic updates during sync
ALTER TABLE trendyol_products SET (
    autovacuum_vacuum_scale_factor = 0.10,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 500,
    autovacuum_analyze_threshold = 250
);

-- trendyol_invoices: Daily financial sync
ALTER TABLE trendyol_invoices SET (
    autovacuum_vacuum_scale_factor = 0.10,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 200,
    autovacuum_analyze_threshold = 100
);

-- store_expenses: Regular updates from users
ALTER TABLE store_expenses SET (
    autovacuum_vacuum_scale_factor = 0.10,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 100,
    autovacuum_analyze_threshold = 50
);

-- alert_history: User alerts
ALTER TABLE alert_history SET (
    autovacuum_vacuum_scale_factor = 0.10,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 200,
    autovacuum_analyze_threshold = 100
);

-- ============================================
-- FILL FACTOR OPTIMIZATION
-- ============================================
-- Tables with frequent updates benefit from lower fill factor
-- This leaves room for updates without page splits

-- trendyol_orders: Order status updates are common
ALTER TABLE trendyol_orders SET (fillfactor = 90);

-- webhook_events: Processing status updates
ALTER TABLE webhook_events SET (fillfactor = 90);

-- email_queue: Status updates during sending
ALTER TABLE email_queue SET (fillfactor = 85);

-- sync_tasks: Frequent status/progress updates
ALTER TABLE sync_tasks SET (fillfactor = 85);

-- trendyol_products: Stock/price updates
ALTER TABLE trendyol_products SET (fillfactor = 90);

-- ============================================
-- ANALYZE CRITICAL TABLES
-- ============================================
ANALYZE trendyol_orders;
ANALYZE trendyol_products;
ANALYZE webhook_events;
ANALYZE email_queue;
ANALYZE activity_logs;

-- ============================================
-- HELPER FUNCTION: Check table bloat
-- ============================================
CREATE OR REPLACE FUNCTION check_table_bloat()
RETURNS TABLE(
    table_name TEXT,
    table_size_mb NUMERIC,
    dead_tuples BIGINT,
    live_tuples BIGINT,
    dead_ratio NUMERIC,
    last_vacuum TIMESTAMP,
    last_analyze TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        schemaname || '.' || relname AS table_name,
        ROUND(pg_table_size(relid) / 1024.0 / 1024.0, 2) AS table_size_mb,
        n_dead_tup AS dead_tuples,
        n_live_tup AS live_tuples,
        CASE WHEN n_live_tup > 0
             THEN ROUND(n_dead_tup::NUMERIC / n_live_tup * 100, 2)
             ELSE 0 END AS dead_ratio,
        last_autovacuum AS last_vacuum,
        last_autoanalyze AS last_analyze
    FROM pg_stat_user_tables
    WHERE relname IN (
        'trendyol_orders', 'trendyol_products', 'webhook_events',
        'email_queue', 'activity_logs', 'sync_tasks', 'trendyol_invoices'
    )
    ORDER BY dead_tuples DESC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_table_bloat IS
    'Returns table bloat statistics for monitoring. High dead_ratio indicates need for manual vacuum.';
