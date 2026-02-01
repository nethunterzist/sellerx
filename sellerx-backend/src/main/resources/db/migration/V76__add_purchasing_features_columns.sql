-- HS Code (Harmonized System / Gumruk Tarife Kodu) for items
ALTER TABLE purchase_order_items ADD COLUMN hs_code VARCHAR(20);

-- Multi-currency support for purchase orders
ALTER TABLE purchase_orders ADD COLUMN supplier_currency VARCHAR(3) DEFAULT 'TRY';
ALTER TABLE purchase_orders ADD COLUMN exchange_rate DECIMAL(12,6);

-- Item cost in supplier currency
ALTER TABLE purchase_order_items ADD COLUMN manufacturing_cost_supplier_currency DECIMAL(10,2);

-- Parent PO reference (for split PO feature)
ALTER TABLE purchase_orders ADD COLUMN parent_po_id BIGINT REFERENCES purchase_orders(id);

-- Item labels (comma-separated tags)
ALTER TABLE purchase_order_items ADD COLUMN labels TEXT;

CREATE INDEX idx_purchase_orders_parent_po_id ON purchase_orders(parent_po_id);
