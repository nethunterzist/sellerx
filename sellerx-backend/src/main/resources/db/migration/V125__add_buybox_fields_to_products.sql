ALTER TABLE trendyol_products ADD COLUMN buybox_order INTEGER;
ALTER TABLE trendyol_products ADD COLUMN buybox_price NUMERIC(10,2);
ALTER TABLE trendyol_products ADD COLUMN has_multiple_seller BOOLEAN DEFAULT false;
ALTER TABLE trendyol_products ADD COLUMN buybox_updated_at TIMESTAMP;

CREATE INDEX idx_products_buybox_order ON trendyol_products(store_id, buybox_order)
    WHERE buybox_order IS NOT NULL;
