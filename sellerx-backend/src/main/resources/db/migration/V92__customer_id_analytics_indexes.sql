-- Migrate customer analytics from customer_email to customer_id
-- Trendyol masks emails with unique hashes per order, making email-based grouping useless.
-- customer_id is a stable numeric identifier that correctly identifies repeat customers.

-- New customer_id based analytics indexes
CREATE INDEX idx_orders_customer_id ON trendyol_orders (store_id, customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_orders_store_date_customer_id ON trendyol_orders (store_id, order_date, customer_id);

-- Drop old email-based analytics indexes
DROP INDEX IF EXISTS idx_orders_customer_email;
DROP INDEX IF EXISTS idx_orders_store_date_customer;
