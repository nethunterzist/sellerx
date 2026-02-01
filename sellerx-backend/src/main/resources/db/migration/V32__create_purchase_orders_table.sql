-- Purchase Orders table for tracking inventory purchases
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    po_number VARCHAR(100) NOT NULL,
    po_date DATE NOT NULL,
    estimated_arrival DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',  -- DRAFT, ORDERED, SHIPPED, CLOSED
    supplier_name VARCHAR(255),  -- Simple text field, no separate suppliers table
    carrier VARCHAR(255),
    tracking_number VARCHAR(255),
    comment TEXT,
    transportation_cost DECIMAL(12,2) DEFAULT 0,
    total_cost DECIMAL(12,2) DEFAULT 0,
    total_units INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(store_id, po_number)
);

-- Indexes for common queries
CREATE INDEX idx_purchase_orders_store_id ON purchase_orders(store_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX idx_purchase_orders_po_date ON purchase_orders(po_date);
