-- V50__create_trendyol_cargo_invoices_table.sql
-- Create table for Trendyol Cargo Invoice details (actual shipping costs per order)

CREATE TABLE IF NOT EXISTS trendyol_cargo_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    invoice_serial_number VARCHAR(100) NOT NULL,
    order_number VARCHAR(50),
    shipment_package_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    desi INTEGER,
    shipment_package_type VARCHAR(50),
    vat_rate INTEGER DEFAULT 20,
    vat_amount DECIMAL(10,2),
    invoice_date DATE,
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Unique constraint to prevent duplicates
CREATE UNIQUE INDEX IF NOT EXISTS idx_cargo_invoices_unique
    ON trendyol_cargo_invoices(store_id, invoice_serial_number, COALESCE(shipment_package_id, 0));

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_cargo_store_order
    ON trendyol_cargo_invoices(store_id, order_number);

CREATE INDEX IF NOT EXISTS idx_cargo_store_date
    ON trendyol_cargo_invoices(store_id, invoice_date);

CREATE INDEX IF NOT EXISTS idx_cargo_shipment_package
    ON trendyol_cargo_invoices(store_id, shipment_package_id);

-- Add comments
COMMENT ON TABLE trendyol_cargo_invoices IS 'Actual cargo/shipping costs per order from Trendyol cargo-invoice API';
COMMENT ON COLUMN trendyol_cargo_invoices.amount IS 'Actual cargo cost charged by Trendyol';
COMMENT ON COLUMN trendyol_cargo_invoices.desi IS 'Dimensional weight used for shipping calculation';
COMMENT ON COLUMN trendyol_cargo_invoices.vat_amount IS 'VAT on cargo cost (typically 20%)';
