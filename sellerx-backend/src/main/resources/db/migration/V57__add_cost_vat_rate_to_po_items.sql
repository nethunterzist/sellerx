-- Add VAT rate tracking to purchase order items for FIFO cost allocation
-- Default value is 20% (standard Turkish VAT rate)

ALTER TABLE purchase_order_items
ADD COLUMN cost_vat_rate INTEGER DEFAULT 20;

-- Update existing records to use the product's VAT rate if available
UPDATE purchase_order_items poi
SET cost_vat_rate = COALESCE(
    (SELECT tp.vat_rate FROM trendyol_products tp WHERE tp.id = poi.product_id),
    20
)
WHERE poi.cost_vat_rate IS NULL OR poi.cost_vat_rate = 20;

COMMENT ON COLUMN purchase_order_items.cost_vat_rate IS 'VAT rate applied to the cost (default 20% for Turkey)';
