-- Create return_records table for detailed return analytics
CREATE TABLE return_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES trendyol_orders(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    barcode VARCHAR(100) NOT NULL,
    product_name VARCHAR(500),
    quantity INTEGER NOT NULL DEFAULT 1,

    -- Cost breakdown
    product_cost DECIMAL(10,2) DEFAULT 0,
    shipping_cost_out DECIMAL(10,2) DEFAULT 0,
    shipping_cost_return DECIMAL(10,2) DEFAULT 0,
    commission_loss DECIMAL(10,2) DEFAULT 0,
    packaging_cost DECIMAL(10,2) DEFAULT 0,
    total_loss DECIMAL(10,2) DEFAULT 0,

    -- Return info
    return_reason VARCHAR(500),
    return_reason_code VARCHAR(50),
    return_date TIMESTAMP,
    return_status VARCHAR(50) DEFAULT 'RECEIVED',

    -- Commission refund tracking
    commission_refunded BOOLEAN DEFAULT FALSE,
    refund_date TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_return_records_store_id ON return_records(store_id);
CREATE INDEX idx_return_records_store_date ON return_records(store_id, return_date);
CREATE INDEX idx_return_records_barcode ON return_records(barcode);
CREATE INDEX idx_return_records_order_id ON return_records(order_id);
CREATE INDEX idx_return_records_return_status ON return_records(return_status);

-- Comment
COMMENT ON TABLE return_records IS 'Detailed return records for analytics and loss tracking';
