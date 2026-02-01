-- V39: E-Invoices Table (Paraşüt Integration)
-- Turkish electronic invoice records for tax compliance

CREATE TABLE e_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL UNIQUE REFERENCES invoices(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Paraşüt integration
    parasut_id VARCHAR(100),  -- Paraşüt invoice ID
    parasut_contact_id VARCHAR(100),  -- Paraşüt customer contact ID

    -- E-invoice status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    -- Status: DRAFT, PENDING, SENT, APPROVED, REJECTED, CANCELLED

    -- Invoice type (Turkish tax system)
    invoice_type VARCHAR(20) NOT NULL,
    -- E_ARSIV: B2C (individuals with TC Kimlik)
    -- E_FATURA: B2B (companies with Vergi No)

    -- Invoice identification
    invoice_series VARCHAR(10) NOT NULL DEFAULT 'SEL',
    invoice_number VARCHAR(50),  -- GİB assigned number

    -- Tax information
    tax_office VARCHAR(100),
    tax_number VARCHAR(20) NOT NULL,  -- TC Kimlik (11 digits) or Vergi No (10 digits)
    company_title VARCHAR(200),  -- Company name for B2B

    -- Document storage
    xml_content TEXT,  -- UBL XML content
    pdf_url VARCHAR(500),  -- Generated PDF URL

    -- Processing timestamps
    sent_at TIMESTAMP,
    approved_at TIMESTAMP,

    -- Error handling
    error_message TEXT,

    -- Additional metadata
    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Billing addresses for e-invoice
CREATE TABLE billing_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Paraşüt contact sync
    parasut_contact_id VARCHAR(100),

    -- Address type
    address_type VARCHAR(20) NOT NULL,  -- INDIVIDUAL, CORPORATE

    -- Contact info
    full_name VARCHAR(200) NOT NULL,
    company_title VARCHAR(200),  -- For corporate
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),

    -- Tax info
    tax_office VARCHAR(100),
    tax_number VARCHAR(20),  -- TC Kimlik or Vergi No

    -- Address
    address_line1 VARCHAR(500) NOT NULL,
    address_line2 VARCHAR(500),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    country VARCHAR(3) NOT NULL DEFAULT 'TR',

    -- Flags
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for e_invoices
CREATE INDEX idx_e_invoices_invoice ON e_invoices(invoice_id);
CREATE INDEX idx_e_invoices_user ON e_invoices(user_id);
CREATE INDEX idx_e_invoices_status ON e_invoices(status);
CREATE INDEX idx_e_invoices_parasut ON e_invoices(parasut_id) WHERE parasut_id IS NOT NULL;
CREATE INDEX idx_e_invoices_type ON e_invoices(invoice_type);

-- Indexes for billing_addresses
CREATE INDEX idx_billing_addresses_user ON billing_addresses(user_id);
CREATE UNIQUE INDEX idx_billing_addresses_default ON billing_addresses(user_id)
    WHERE is_default = TRUE;

COMMENT ON TABLE e_invoices IS 'Turkish e-invoice records for Paraşüt integration';
COMMENT ON COLUMN e_invoices.invoice_type IS 'E_ARSIV (B2C) or E_FATURA (B2B)';
COMMENT ON COLUMN e_invoices.tax_number IS '11 digits for TC Kimlik, 10 digits for Vergi No';
COMMENT ON TABLE billing_addresses IS 'User billing addresses for e-invoice generation';
