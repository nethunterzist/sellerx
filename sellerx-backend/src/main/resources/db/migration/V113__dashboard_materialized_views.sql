-- V113: Dashboard Materialized Views
-- Target: Pre-compute dashboard statistics for faster queries at scale
-- These views can be refreshed periodically (every 15-30 minutes)

-- ============================================
-- 1. DAILY ORDER STATS MATERIALIZED VIEW
-- ============================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_order_stats AS
SELECT
    o.store_id,
    DATE_TRUNC('day', o.order_date)::DATE AS period_date,
    COUNT(*) AS order_count,
    COUNT(DISTINCT o.customer_id) AS unique_customers,
    COALESCE(SUM(o.total_price), 0) AS total_revenue,
    COALESCE(SUM(o.gross_amount), 0) AS gross_amount,
    COALESCE(SUM(o.total_discount), 0) AS total_discount,
    COALESCE(SUM(o.total_ty_discount), 0) AS total_ty_discount,
    COALESCE(SUM(o.estimated_commission), 0) AS total_commission,
    COALESCE(SUM(o.coupon_discount), 0) AS total_coupon_discount,
    COALESCE(SUM(o.early_payment_fee), 0) AS total_early_payment_fee,
    COUNT(CASE WHEN o.status = 'Delivered' THEN 1 END) AS delivered_count,
    COUNT(CASE WHEN o.status = 'Cancelled' THEN 1 END) AS cancelled_count,
    COUNT(CASE WHEN o.status = 'Returned' THEN 1 END) AS returned_count
FROM trendyol_orders o
WHERE o.order_date >= NOW() - INTERVAL '2 years'
GROUP BY o.store_id, DATE_TRUNC('day', o.order_date)::DATE
WITH DATA;

-- Index for fast lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_order_stats_pk
    ON mv_daily_order_stats(store_id, period_date);

CREATE INDEX IF NOT EXISTS idx_mv_daily_order_stats_date
    ON mv_daily_order_stats(period_date DESC);

COMMENT ON MATERIALIZED VIEW mv_daily_order_stats IS
    'Pre-computed daily order statistics per store. Refresh every 15-30 minutes.';

-- ============================================
-- 2. MONTHLY ORDER STATS MATERIALIZED VIEW
-- ============================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_monthly_order_stats AS
SELECT
    o.store_id,
    DATE_TRUNC('month', o.order_date)::DATE AS period_month,
    COUNT(*) AS order_count,
    COUNT(DISTINCT o.customer_id) AS unique_customers,
    COALESCE(SUM(o.total_price), 0) AS total_revenue,
    COALESCE(SUM(o.gross_amount), 0) AS gross_amount,
    COALESCE(SUM(o.estimated_commission), 0) AS total_commission,
    COUNT(CASE WHEN o.status = 'Delivered' THEN 1 END) AS delivered_count,
    COUNT(CASE WHEN o.status = 'Cancelled' THEN 1 END) AS cancelled_count,
    COUNT(CASE WHEN o.status = 'Returned' THEN 1 END) AS returned_count
FROM trendyol_orders o
WHERE o.order_date >= NOW() - INTERVAL '3 years'
GROUP BY o.store_id, DATE_TRUNC('month', o.order_date)::DATE
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_monthly_order_stats_pk
    ON mv_monthly_order_stats(store_id, period_month);

CREATE INDEX IF NOT EXISTS idx_mv_monthly_order_stats_month
    ON mv_monthly_order_stats(period_month DESC);

COMMENT ON MATERIALIZED VIEW mv_monthly_order_stats IS
    'Pre-computed monthly order statistics per store. Refresh hourly.';

-- ============================================
-- 3. CITY-BASED SALES MATERIALIZED VIEW
-- ============================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_city_sales_stats AS
SELECT
    o.store_id,
    o.shipment_city,
    DATE_TRUNC('month', o.order_date)::DATE AS period_month,
    COUNT(*) AS order_count,
    COUNT(DISTINCT o.customer_id) AS unique_customers,
    COALESCE(SUM(o.total_price), 0) AS total_revenue
FROM trendyol_orders o
WHERE o.order_date >= NOW() - INTERVAL '1 year'
  AND o.shipment_city IS NOT NULL
GROUP BY o.store_id, o.shipment_city, DATE_TRUNC('month', o.order_date)::DATE
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_city_sales_pk
    ON mv_city_sales_stats(store_id, shipment_city, period_month);

CREATE INDEX IF NOT EXISTS idx_mv_city_sales_store_month
    ON mv_city_sales_stats(store_id, period_month DESC);

COMMENT ON MATERIALIZED VIEW mv_city_sales_stats IS
    'Pre-computed city-based sales statistics. Useful for geographic analysis.';

-- ============================================
-- 4. PRODUCT PERFORMANCE MATERIALIZED VIEW
-- ============================================
-- This aggregates order item data from JSONB for product performance analysis
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_product_performance AS
SELECT
    o.store_id,
    (item->>'barcode')::VARCHAR AS barcode,
    DATE_TRUNC('month', o.order_date)::DATE AS period_month,
    COUNT(*) AS sale_count,
    COALESCE(SUM((item->>'quantity')::INTEGER), 0) AS total_quantity,
    COALESCE(SUM((item->>'price')::DECIMAL), 0) AS total_revenue,
    COALESCE(SUM((item->>'vatBaseAmount')::DECIMAL *
        COALESCE((SELECT p.last_commission_rate FROM trendyol_products p
                  WHERE p.store_id = o.store_id AND p.barcode = (item->>'barcode')::VARCHAR LIMIT 1), 0) / 100), 0) AS total_commission
FROM trendyol_orders o,
     jsonb_array_elements(o.order_items) AS item
WHERE o.order_date >= NOW() - INTERVAL '6 months'
  AND o.status NOT IN ('Cancelled', 'Returned')
GROUP BY o.store_id, (item->>'barcode')::VARCHAR, DATE_TRUNC('month', o.order_date)::DATE
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_product_performance_pk
    ON mv_product_performance(store_id, barcode, period_month);

CREATE INDEX IF NOT EXISTS idx_mv_product_performance_store
    ON mv_product_performance(store_id, period_month DESC);

COMMENT ON MATERIALIZED VIEW mv_product_performance IS
    'Pre-computed product performance metrics from order items. Refresh hourly.';

-- ============================================
-- 5. REFRESH FUNCTION FOR ALL VIEWS
-- ============================================
CREATE OR REPLACE FUNCTION refresh_dashboard_views(concurrent BOOLEAN DEFAULT TRUE)
RETURNS TABLE(view_name TEXT, refresh_time_ms BIGINT) AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
BEGIN
    -- Refresh daily stats
    start_time := clock_timestamp();
    IF concurrent THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_order_stats;
    ELSE
        REFRESH MATERIALIZED VIEW mv_daily_order_stats;
    END IF;
    end_time := clock_timestamp();
    view_name := 'mv_daily_order_stats';
    refresh_time_ms := EXTRACT(MILLISECONDS FROM (end_time - start_time))::BIGINT;
    RETURN NEXT;

    -- Refresh monthly stats
    start_time := clock_timestamp();
    IF concurrent THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_order_stats;
    ELSE
        REFRESH MATERIALIZED VIEW mv_monthly_order_stats;
    END IF;
    end_time := clock_timestamp();
    view_name := 'mv_monthly_order_stats';
    refresh_time_ms := EXTRACT(MILLISECONDS FROM (end_time - start_time))::BIGINT;
    RETURN NEXT;

    -- Refresh city sales
    start_time := clock_timestamp();
    IF concurrent THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_city_sales_stats;
    ELSE
        REFRESH MATERIALIZED VIEW mv_city_sales_stats;
    END IF;
    end_time := clock_timestamp();
    view_name := 'mv_city_sales_stats';
    refresh_time_ms := EXTRACT(MILLISECONDS FROM (end_time - start_time))::BIGINT;
    RETURN NEXT;

    -- Refresh product performance
    start_time := clock_timestamp();
    IF concurrent THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_product_performance;
    ELSE
        REFRESH MATERIALIZED VIEW mv_product_performance;
    END IF;
    end_time := clock_timestamp();
    view_name := 'mv_product_performance';
    refresh_time_ms := EXTRACT(MILLISECONDS FROM (end_time - start_time))::BIGINT;
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_dashboard_views IS
    'Refreshes all dashboard materialized views. Use concurrent=TRUE for non-blocking refresh (requires unique index).';

-- ============================================
-- 6. HELPER VIEW FOR STORE DASHBOARD QUICK STATS
-- ============================================
CREATE OR REPLACE VIEW v_store_quick_stats AS
SELECT
    s.id AS store_id,
    s.store_name,
    COALESCE(today.order_count, 0) AS today_orders,
    COALESCE(today.total_revenue, 0) AS today_revenue,
    COALESCE(yesterday.order_count, 0) AS yesterday_orders,
    COALESCE(yesterday.total_revenue, 0) AS yesterday_revenue,
    COALESCE(month.order_count, 0) AS month_orders,
    COALESCE(month.total_revenue, 0) AS month_revenue
FROM stores s
LEFT JOIN mv_daily_order_stats today
    ON s.id = today.store_id AND today.period_date = CURRENT_DATE
LEFT JOIN mv_daily_order_stats yesterday
    ON s.id = yesterday.store_id AND yesterday.period_date = CURRENT_DATE - 1
LEFT JOIN mv_monthly_order_stats month
    ON s.id = month.store_id AND month.period_month = DATE_TRUNC('month', CURRENT_DATE)::DATE;

COMMENT ON VIEW v_store_quick_stats IS
    'Quick stats view for store dashboards. Uses materialized views for fast access.';
