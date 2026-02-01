-- V65: Create trendyol_deduction_invoices table for platform fees and other deductions
-- This table stores all DeductionInvoices from Trendyol OtherFinancials API
-- Including: Platform Hizmet Bedeli, Uluslararası Hizmet Bedeli, Yurt Dışı Operasyon Bedeli, etc.

CREATE TABLE IF NOT EXISTS trendyol_deduction_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Trendyol API fields
    trendyol_id VARCHAR(100) NOT NULL,                -- API "id" field (e.g., "DDF2025015689931")
    transaction_date TIMESTAMP NOT NULL,              -- Transaction timestamp
    transaction_type VARCHAR(100) NOT NULL,           -- e.g., "Platform Hizmet Bedeli", "Kargo Fatura", "Reklam Bedeli"
    description TEXT,                                 -- Full description from API

    -- Financial data
    debt NUMERIC(12, 2) DEFAULT 0,                    -- Amount charged (debt)
    credit NUMERIC(12, 2) DEFAULT 0,                  -- Credit amount (if any)

    -- Invoice details
    invoice_serial_number VARCHAR(100),               -- Same as trendyol_id in most cases
    payment_order_id BIGINT,                          -- Associated payment order ID
    payment_date TIMESTAMP,                           -- Payment date

    -- Additional metadata
    order_number VARCHAR(50),                         -- Related order number (if applicable)
    shipment_package_id BIGINT,                       -- Related shipment (for cargo invoices)
    seller_id BIGINT,                                 -- Trendyol seller ID
    country VARCHAR(50) DEFAULT 'Türkiye',            -- Country
    currency VARCHAR(10) DEFAULT 'TRY',               -- Currency
    affiliate VARCHAR(50),                            -- Affiliate (e.g., "TRENDYOLTR")

    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),

    -- Unique constraint to prevent duplicates
    CONSTRAINT uk_deduction_invoice UNIQUE (store_id, trendyol_id)
);

-- Indexes for efficient querying
CREATE INDEX idx_deduction_store_date ON trendyol_deduction_invoices(store_id, transaction_date);
CREATE INDEX idx_deduction_type ON trendyol_deduction_invoices(store_id, transaction_type);
CREATE INDEX idx_deduction_payment_order ON trendyol_deduction_invoices(store_id, payment_order_id);

-- Comment on table
COMMENT ON TABLE trendyol_deduction_invoices IS 'Stores all DeductionInvoices from Trendyol OtherFinancials API including platform fees, cargo fees, ad fees, etc.';
COMMENT ON COLUMN trendyol_deduction_invoices.transaction_type IS 'Invoice type: Platform Hizmet Bedeli, Kargo Fatura, Reklam Bedeli, Uluslararası Hizmet Bedeli, Yurt Dışı Operasyon Bedeli, Komisyon Faturası, Kusurlu Ürün Faturası, Erken Ödeme Kesinti Faturası, etc.';
