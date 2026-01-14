-- Create trendyol_orders table for storing Trendyol order data
CREATE TABLE IF NOT EXISTS trendyol_orders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id UUID NOT NULL,
    ty_order_number VARCHAR(255) NOT NULL,
    package_no BIGINT NOT NULL, -- This is the "id" field from Trendyol API, which is the package number
    order_date TIMESTAMP NOT NULL, -- Converted from originShipmentDate milliseconds
    gross_amount DECIMAL(10,2) NOT NULL,
    total_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_ty_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    order_items JSONB NOT NULL DEFAULT '[]'::jsonb, -- Array of order items with product details
    shipment_package_status VARCHAR(100),
    status VARCHAR(100),
    cargo_deci INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_trendyol_orders_store_id ON trendyol_orders(store_id);
CREATE INDEX idx_trendyol_orders_ty_order_number ON trendyol_orders(ty_order_number);
CREATE INDEX idx_trendyol_orders_package_no ON trendyol_orders(package_no);
CREATE INDEX idx_trendyol_orders_order_date ON trendyol_orders(order_date);
CREATE INDEX idx_trendyol_orders_status ON trendyol_orders(status);

-- Create unique constraint on store_id and package_no combination
-- This ensures we don't duplicate the same package for the same store
CREATE UNIQUE INDEX idx_trendyol_orders_store_package_unique ON trendyol_orders(store_id, package_no);

-- Add comment for JSONB structure documentation
COMMENT ON COLUMN trendyol_orders.order_items IS 'JSON array containing order items: [{"barcode": "123", "productName": "Product Name", "quantity": 1, "unitPriceOrder": 100.00, "unitPriceDiscount": 5.00, "unitPriceTyDiscount": 0.00, "vatBaseAmount": 20.00, "price": 95.00, "cost": 50.00, "costVat": 18}]';
