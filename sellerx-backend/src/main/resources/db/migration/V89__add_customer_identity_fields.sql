-- Add customer identity fields to trendyol_orders for repeat purchase analysis
ALTER TABLE trendyol_orders ADD COLUMN customer_first_name VARCHAR(255);
ALTER TABLE trendyol_orders ADD COLUMN customer_last_name VARCHAR(255);
ALTER TABLE trendyol_orders ADD COLUMN customer_email VARCHAR(255);
ALTER TABLE trendyol_orders ADD COLUMN customer_id BIGINT;

-- Index for customer analytics grouping by email
CREATE INDEX idx_orders_customer_email ON trendyol_orders (store_id, customer_email) WHERE customer_email IS NOT NULL;

-- Composite index for date-based customer analytics
CREATE INDEX idx_orders_store_date_customer ON trendyol_orders (store_id, order_date, customer_email);
