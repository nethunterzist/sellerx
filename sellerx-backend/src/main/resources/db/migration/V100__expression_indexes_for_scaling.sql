-- V100: Expression indexes for JSONB columns and composite indexes for scaling
-- Target: 1500+ stores, 225M+ orders/year
-- Note: Using regular CREATE INDEX (not CONCURRENTLY) to avoid Flyway transaction issues

-- ============================================
-- JSONB EXPRESSION INDEXES
-- ============================================

-- 1. Order number search (most frequently used)
-- Used in: Customer support, dashboard order search
-- Query pattern: WHERE order_items->0->>'orderNumber' = 'X'
CREATE INDEX IF NOT EXISTS idx_orders_order_number_expr
    ON trendyol_orders USING btree ((order_items->0->>'orderNumber'));

-- 2. Line Item ID search (Trendyol reference)
-- Used in: Trendyol API reconciliation
CREATE INDEX IF NOT EXISTS idx_orders_line_item_id_expr
    ON trendyol_orders USING btree ((order_items->0->>'lineItemId'));

-- 3. Barcode search within order items (jsonb_path_ops for containment)
-- Used in: Product-based order queries
-- Query pattern: WHERE order_items @> '[{"barcode": "X"}]'
CREATE INDEX IF NOT EXISTS idx_orders_barcode_jsonb
    ON trendyol_orders USING gin ((order_items) jsonb_path_ops);

-- 4. Product last cost (from cost_and_stock_info array)
-- Used in: Profit calculation, cost reporting
CREATE INDEX IF NOT EXISTS idx_products_last_cost_expr
    ON trendyol_products USING btree ((cost_and_stock_info->-1->>'unitCost'));

-- ============================================
-- COMPOSITE INDEXES FOR DASHBOARD QUERIES
-- ============================================

-- 5. Store + date + status (most common dashboard filter)
-- Covers: Order list with date and status filters
CREATE INDEX IF NOT EXISTS idx_orders_store_date_status_composite
    ON trendyol_orders(store_id, order_date DESC, status);

-- 6. Store + city + date (geographic analysis)
-- Used in: City-based sales analytics
CREATE INDEX IF NOT EXISTS idx_orders_store_city_date
    ON trendyol_orders(store_id, shipment_city, order_date DESC);

-- 7. Customer lifecycle index (customer analytics)
-- Used in: Customer retention analysis, repeat purchase detection
CREATE INDEX IF NOT EXISTS idx_orders_customer_lifecycle
    ON trendyol_orders(store_id, customer_id, order_date DESC);

-- ============================================
-- INVOICE INDEXES
-- ============================================

-- 8. Invoice category + date (financial reports)
-- Used in: Invoice categorization, financial summaries
CREATE INDEX IF NOT EXISTS idx_invoices_category_date
    ON trendyol_invoices(store_id, invoice_category, invoice_date DESC);

-- 9. Invoice number search
CREATE INDEX IF NOT EXISTS idx_invoices_number
    ON trendyol_invoices(store_id, invoice_number);

-- ============================================
-- PARTIAL INDEX (OPTIONAL - for recent data optimization)
-- ============================================

-- 10. Recent orders only (last 90 days) - smaller index, faster queries
-- This is commented out as it requires maintenance (recreation periodically)
-- CREATE INDEX IF NOT EXISTS idx_orders_recent_order_number
--     ON trendyol_orders USING btree ((order_items->0->>'orderNumber'))
--     WHERE order_date > CURRENT_DATE - INTERVAL '90 days';

-- ============================================
-- STATISTICS UPDATE
-- ============================================

-- Analyze tables to update statistics for query planner
ANALYZE trendyol_orders;
ANALYZE trendyol_products;
ANALYZE trendyol_invoices;
