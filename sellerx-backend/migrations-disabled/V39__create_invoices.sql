-- Invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Invoice identification
    invoice_number VARCHAR(50) NOT NULL UNIQUE,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Amounts
    subtotal DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 20.00,
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'TRY',

    -- Billing period
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,

    -- Invoice details
    line_items JSONB DEFAULT '[]',
    notes TEXT,

    -- Due date
    due_date TIMESTAMP,

    -- Payment tracking
    paid_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Transactions
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_method_id UUID REFERENCES payment_methods(id),

    -- Amount
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'TRY',

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- iyzico data
    iyzico_payment_id VARCHAR(200),
    iyzico_conversation_id VARCHAR(200),
    provider_response JSONB,

    -- Failure info
    failure_code VARCHAR(100),
    failure_message TEXT,

    -- Retry tracking
    attempt_number INTEGER DEFAULT 1,
    next_retry_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoices_user ON invoices(user_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status = 'PENDING';

CREATE INDEX idx_payment_transactions_invoice ON payment_transactions(invoice_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_retry ON payment_transactions(next_retry_at) WHERE next_retry_at IS NOT NULL;
CREATE INDEX idx_payment_transactions_iyzico ON payment_transactions(iyzico_payment_id);
CREATE INDEX idx_payment_transactions_conversation ON payment_transactions(iyzico_conversation_id);
