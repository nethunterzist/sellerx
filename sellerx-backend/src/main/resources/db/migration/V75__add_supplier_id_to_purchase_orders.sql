-- Link purchase orders to supplier entity
ALTER TABLE purchase_orders ADD COLUMN supplier_id BIGINT REFERENCES suppliers(id);

CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders(supplier_id);
