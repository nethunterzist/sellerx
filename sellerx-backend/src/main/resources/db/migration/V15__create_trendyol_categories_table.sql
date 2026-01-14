-- Create trendyol_categories table
CREATE TABLE IF NOT EXISTS trendyol_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id BIGINT NOT NULL UNIQUE,
    category_name VARCHAR(500) NOT NULL,
    parent_category VARCHAR(500),
    commission_rate DECIMAL(5,2),
    average_shipment_size DECIMAL(5,2)
);

-- Create indexes for better performance
CREATE INDEX idx_trendyol_categories_category_id ON trendyol_categories(category_id);
CREATE INDEX idx_trendyol_categories_category_name ON trendyol_categories(category_name);
CREATE INDEX idx_trendyol_categories_parent_category ON trendyol_categories(parent_category);

-- Add comments
COMMENT ON TABLE trendyol_categories IS 'Trendyol category information';
COMMENT ON COLUMN trendyol_categories.id IS 'UUID primary key';
COMMENT ON COLUMN trendyol_categories.category_id IS 'Trendyol category ID';
COMMENT ON COLUMN trendyol_categories.category_name IS 'Category name';
COMMENT ON COLUMN trendyol_categories.parent_category IS 'Parent category name';
COMMENT ON COLUMN trendyol_categories.commission_rate IS 'Commission rate percentage';
COMMENT ON COLUMN trendyol_categories.average_shipment_size IS 'Average shipment size for this category';