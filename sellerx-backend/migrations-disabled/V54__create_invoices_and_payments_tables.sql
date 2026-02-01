-- V38: Invoices and Payment Transactions Tables
-- Invoice records and payment transaction history

-- Invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Invoice identification
    invoice_number VARCHAR(50) NOT NULL UNIQUE,  -- Format: INV-2026-000001
    invoice_series VARCHAR(10) NOT NULL DEFAULT 'SEL',

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, PENDING, PAID, FAILED, REFUNDED, VOID

    -- Amounts (in TRY)
    subtotal DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL DEFAULT 20,  -- KDV %20
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Billing period this invoice covers
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,

    -- Payment timing
    due_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,

    -- Line items (JSONB for flexibility)
    line_items JSONB NOT NULL DEFAULT '[]',
    -- Example: [{"description": "Pro Plan - Aylık", "quantity": 1, "unitPrice": 599, "amount": 599}]

    -- Billing address snapshot
    billing_address JSONB,

    -- Notes
    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Payment transactions table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_method_id UUID REFERENCES payment_methods(id),

    -- Amount
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- Transaction status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED

    -- iyzico transaction details
    provider VARCHAR(50) NOT NULL DEFAULT 'iyzico',
    iyzico_payment_id VARCHAR(255),
    iyzico_conversation_id VARCHAR(255),  -- Idempotency key
    iyzico_payment_transaction_id VARCHAR(255),

    -- Full provider response (for debugging)
    provider_response JSONB,

    -- Error tracking
    failure_code VARCHAR(100),
    failure_message TEXT,

    -- Retry tracking
    attempt_number INTEGER NOT NULL DEFAULT 1,
    next_retry_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for invoices
CREATE INDEX idx_invoices_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoices_user ON invoices(user_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status = 'PENDING';
CREATE INDEX idx_invoices_number ON invoices(invoice_number);

-- Indexes for payment_transactions
CREATE INDEX idx_payment_transactions_invoice ON payment_transactions(invoice_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_retry ON payment_transactions(next_retry_at)
    WHERE status = 'FAILED' AND attempt_number < 3;
CREATE UNIQUE INDEX idx_payment_transactions_iyzico_id ON payment_transactions(iyzico_payment_id)
    WHERE iyzico_payment_id IS NOT NULL;
CREATE UNIQUE INDEX idx_payment_transactions_conversation ON payment_transactions(iyzico_conversation_id)
    WHERE iyzico_conversation_id IS NOT NULL;

-- Sequence for invoice numbers
CREATE SEQUENCE invoice_number_seq START 1;

COMMENT ON TABLE invoices IS 'Invoice records for subscription payments';
COMMENT ON COLUMN invoices.line_items IS 'JSONB array of invoice line items';
COMMENT ON TABLE payment_transactions IS 'Payment transaction history with iyzico integration';
COMMENT ON COLUMN payment_transactions.iyzico_conversation_id IS 'Idempotency key for iyzico requests';
