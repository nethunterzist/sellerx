-- E-Invoices table (Parasut integration for Turkish e-fatura)
CREATE TABLE e_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL UNIQUE REFERENCES invoices(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parasut_id VARCHAR(100),
    parasut_contact_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    invoice_type VARCHAR(20) NOT NULL,
    invoice_series VARCHAR(10) NOT NULL DEFAULT 'SEL',
    invoice_number VARCHAR(50),
    tax_office VARCHAR(100),
    tax_number VARCHAR(20) NOT NULL,
    company_title VARCHAR(200),
    xml_content TEXT,
    pdf_url VARCHAR(500),
    sent_at TIMESTAMP,
    approved_at TIMESTAMP,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_e_invoices_invoice_id ON e_invoices(invoice_id);
CREATE INDEX idx_e_invoices_user_id ON e_invoices(user_id);
CREATE INDEX idx_e_invoices_status ON e_invoices(status);
CREATE INDEX idx_e_invoices_parasut_id ON e_invoices(parasut_id);
CREATE INDEX idx_e_invoices_tax_number ON e_invoices(tax_number);

-- Add check constraint for e-invoice status
ALTER TABLE e_invoices ADD CONSTRAINT chk_e_invoice_status
    CHECK (status IN ('DRAFT', 'PENDING', 'SENT', 'APPROVED', 'ERROR'));

-- Add check constraint for invoice type (E_ARSIV=B2C, E_FATURA=B2B)
ALTER TABLE e_invoices ADD CONSTRAINT chk_e_invoice_type
    CHECK (invoice_type IN ('E_ARSIV', 'E_FATURA'));
