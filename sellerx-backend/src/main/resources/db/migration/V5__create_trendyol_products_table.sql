-- Create trendyol_products table
CREATE TABLE IF NOT EXISTS trendyol_products (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    barcode VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    category_name VARCHAR(255),
    create_date_time BIGINT,
    has_active_campaign BOOLEAN DEFAULT false,
    brand VARCHAR(255),
    brand_id BIGINT,
    product_main_id VARCHAR(255),
    image TEXT,
    product_url TEXT,
    dimensional_weight DECIMAL(10,2),
    sale_price DECIMAL(10,2),
    vat_rate INTEGER,
    quantity INTEGER DEFAULT 0,
    cost_and_stock_info JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    UNIQUE(store_id, product_id)
);

-- Create index for better performance
CREATE INDEX idx_trendyol_products_store_id ON trendyol_products(store_id);
CREATE INDEX idx_trendyol_products_product_id ON trendyol_products(product_id);
CREATE INDEX idx_trendyol_products_barcode ON trendyol_products(barcode);

-- Add comments for JSONB structure documentation
COMMENT ON COLUMN trendyol_products.cost_and_stock_info IS 'JSON array containing cost and stock history: [{"quantity": 100, "unitCost": 25.50, "costVatRate": 18, "stockDate": "2024-01-01T10:00:00Z"}]';
