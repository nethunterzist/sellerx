-- Create trendyol_invoices table for unified invoice management
CREATE TABLE trendyol_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_type VARCHAR(100) NOT NULL,
    invoice_type_code VARCHAR(50) NOT NULL,
    invoice_category VARCHAR(30) NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    vat_amount DECIMAL(15, 2) DEFAULT 0,
    vat_rate DECIMAL(5, 2) DEFAULT 0,
    base_amount DECIMAL(15, 2),
    is_deduction BOOLEAN NOT NULL DEFAULT TRUE,
    country VARCHAR(50) DEFAULT 'Türkiye',
    currency VARCHAR(10) DEFAULT 'TRY',
    order_number VARCHAR(50),
    shipment_package_id BIGINT,
    payment_order_id BIGINT,
    barcode VARCHAR(100),
    product_name VARCHAR(500),
    desi DECIMAL(10, 2),
    description TEXT,
    details JSONB,
    trendyol_invoice_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_invoice UNIQUE (store_id, invoice_number)
);

-- Create indexes for efficient querying
CREATE INDEX idx_invoice_store_type ON trendyol_invoices(store_id, invoice_type_code);
CREATE INDEX idx_invoice_store_date ON trendyol_invoices(store_id, invoice_date);
CREATE INDEX idx_invoice_type_code ON trendyol_invoices(invoice_type_code);
CREATE INDEX idx_invoice_category ON trendyol_invoices(invoice_category);
CREATE INDEX idx_invoice_date ON trendyol_invoices(invoice_date);

-- Add comments for documentation
COMMENT ON TABLE trendyol_invoices IS 'Stores all invoice types from Trendyol (deductions and refunds)';
COMMENT ON COLUMN trendyol_invoices.invoice_type IS 'Original invoice type name from Trendyol (e.g., "Kargo Fatura")';
COMMENT ON COLUMN trendyol_invoices.invoice_type_code IS 'Normalized type code (e.g., KARGO_FATURA)';
COMMENT ON COLUMN trendyol_invoices.invoice_category IS 'Category: KOMISYON, KARGO, ULUSLARARASI, CEZA, REKLAM, DIGER, IADE';
COMMENT ON COLUMN trendyol_invoices.is_deduction IS 'TRUE if deducted from seller, FALSE if paid to seller';
COMMENT ON COLUMN trendyol_invoices.details IS 'Additional invoice details in JSONB format';
