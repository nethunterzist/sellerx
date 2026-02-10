-- Add missing indexes identified by Database Agent analysis
-- These indexes improve query performance for common operations
-- Note: Using regular CREATE INDEX (not CONCURRENTLY) to avoid Flyway transaction issues

-- Orders: Improve queries filtering by store_id and order_date
CREATE INDEX IF NOT EXISTS idx_trendyol_orders_store_id_order_date
    ON trendyol_orders(store_id, order_date);

-- Orders: Improve status-based queries
CREATE INDEX IF NOT EXISTS idx_trendyol_orders_status
    ON trendyol_orders(status);

-- Products: Improve barcode lookups
CREATE INDEX IF NOT EXISTS idx_trendyol_products_store_id_barcode
    ON trendyol_products(store_id, barcode);

-- Products: Improve product_main_id lookups
CREATE INDEX IF NOT EXISTS idx_trendyol_products_store_id_product_main_id
    ON trendyol_products(store_id, product_main_id);

-- Webhook events: Improve store-specific event queries
CREATE INDEX IF NOT EXISTS idx_webhook_events_store_id_created_at
    ON webhook_events(store_id, created_at);

-- Expenses: Improve date-based expense queries
CREATE INDEX IF NOT EXISTS idx_store_expenses_store_id_expense_date
    ON store_expenses(store_id, date);

-- Purchase orders: Improve date-based queries
CREATE INDEX IF NOT EXISTS idx_purchase_orders_store_id_order_date
    ON purchase_orders(store_id, po_date);
