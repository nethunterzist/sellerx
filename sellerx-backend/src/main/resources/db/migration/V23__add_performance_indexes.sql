-- Performance indexes for dashboard queries
-- These indexes significantly improve query performance for the most common access patterns

-- Index for order queries filtered by store, date, and status (most common dashboard query pattern)
CREATE INDEX IF NOT EXISTS idx_orders_store_date_status
ON trendyol_orders(store_id, order_date, status);

-- Index for order queries filtered by store and date only
CREATE INDEX IF NOT EXISTS idx_orders_store_date
ON trendyol_orders(store_id, order_date);

-- Index for product lookups by store and barcode (fixes N+1 query problem)
CREATE INDEX IF NOT EXISTS idx_products_store_barcode
ON trendyol_products(store_id, barcode);

-- Index for expense queries by store
CREATE INDEX IF NOT EXISTS idx_expenses_store
ON store_expenses(store_id);

-- Note: Financial settlements index will be added when the table is created