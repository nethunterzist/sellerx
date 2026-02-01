-- Invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_series VARCHAR(10) NOT NULL DEFAULT 'SEL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subtotal DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL DEFAULT 20,
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    line_items JSONB,
    billing_address JSONB,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for invoices
CREATE INDEX idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_created_at ON invoices(created_at);

-- Add check constraint for invoice status
ALTER TABLE invoices ADD CONSTRAINT chk_invoice_status
    CHECK (status IN ('DRAFT', 'PENDING', 'PAID', 'FAILED', 'REFUNDED', 'VOID'));

-- Payment Transactions table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    payment_method_id UUID REFERENCES payment_methods(id),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(50) NOT NULL DEFAULT 'iyzico',
    iyzico_payment_id VARCHAR(500),
    iyzico_conversation_id VARCHAR(500),
    iyzico_payment_transaction_id VARCHAR(500),
    provider_response JSONB,
    failure_code VARCHAR(100),
    failure_message TEXT,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for payment transactions
CREATE INDEX idx_payment_transactions_invoice_id ON payment_transactions(invoice_id);
CREATE INDEX idx_payment_transactions_payment_method_id ON payment_transactions(payment_method_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_iyzico_payment_id ON payment_transactions(iyzico_payment_id);
CREATE INDEX idx_payment_transactions_iyzico_conversation_id ON payment_transactions(iyzico_conversation_id);
CREATE INDEX idx_payment_transactions_next_retry_at ON payment_transactions(next_retry_at) WHERE next_retry_at IS NOT NULL;

-- Add check constraint for payment status
ALTER TABLE payment_transactions ADD CONSTRAINT chk_payment_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'REFUNDED'));

-- Create invoice number sequence
CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;
