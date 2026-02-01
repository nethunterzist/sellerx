-- Purchase Order Items table for tracking products in each purchase order
CREATE TABLE purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES trendyol_products(id) ON DELETE CASCADE,
    units_ordered INTEGER NOT NULL,
    units_per_box INTEGER,
    boxes_ordered INTEGER,
    box_dimensions VARCHAR(100),
    manufacturing_cost_per_unit DECIMAL(10,2) NOT NULL,
    transportation_cost_per_unit DECIMAL(10,2) DEFAULT 0,
    total_cost_per_unit DECIMAL(10,2) GENERATED ALWAYS AS (manufacturing_cost_per_unit + COALESCE(transportation_cost_per_unit, 0)) STORED,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_po_items_purchase_order_id ON purchase_order_items(purchase_order_id);
CREATE INDEX idx_po_items_product_id ON purchase_order_items(product_id);
