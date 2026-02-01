-- E-Invoices (Parasut integration)
CREATE TABLE e_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),

    -- Parasut data
    parasut_id VARCHAR(100),
    parasut_contact_id VARCHAR(100),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Invoice type
    invoice_type VARCHAR(20) NOT NULL DEFAULT 'E_ARSIV',
    invoice_series VARCHAR(10) DEFAULT 'SEL',

    -- PDF
    pdf_url VARCHAR(500),

    -- Customer tax info (for e-fatura)
    tax_number VARCHAR(20),
    tax_office VARCHAR(100),

    -- Timestamps
    sent_at TIMESTAMP,
    approved_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Billing Addresses
CREATE TABLE billing_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Address type
    address_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',

    -- Personal info
    first_name VARCHAR(50),
    last_name VARCHAR(50),

    -- Company info (for corporate)
    company_name VARCHAR(200),
    tax_number VARCHAR(20),
    tax_office VARCHAR(100),

    -- Address
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2) DEFAULT 'TR',

    -- Contact
    phone VARCHAR(20),

    -- Status
    is_default BOOLEAN DEFAULT false,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_e_invoices_invoice ON e_invoices(invoice_id);
CREATE INDEX idx_e_invoices_parasut ON e_invoices(parasut_id);
CREATE INDEX idx_e_invoices_status ON e_invoices(status);

CREATE INDEX idx_billing_addresses_user ON billing_addresses(user_id);
CREATE INDEX idx_billing_addresses_default ON billing_addresses(user_id, is_default) WHERE is_default = true;
