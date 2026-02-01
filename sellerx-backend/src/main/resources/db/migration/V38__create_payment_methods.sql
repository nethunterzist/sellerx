-- Payment Methods table (iyzico card tokenization)
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL DEFAULT 'CREDIT_CARD',
    provider VARCHAR(50) NOT NULL DEFAULT 'iyzico',
    iyzico_card_user_key VARCHAR(500),
    iyzico_card_token VARCHAR(500) NOT NULL,
    card_last_four VARCHAR(4) NOT NULL,
    card_brand VARCHAR(50),
    card_family VARCHAR(100),
    card_holder_name VARCHAR(200),
    card_exp_month INTEGER NOT NULL,
    card_exp_year INTEGER NOT NULL,
    card_bank_name VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_active ON payment_methods(is_active) WHERE is_active = true;
CREATE INDEX idx_payment_methods_default ON payment_methods(user_id, is_default) WHERE is_default = true;
CREATE INDEX idx_payment_methods_iyzico_token ON payment_methods(iyzico_card_token);

-- Add check constraint for payment method type
ALTER TABLE payment_methods ADD CONSTRAINT chk_payment_method_type
    CHECK (type IN ('CREDIT_CARD', 'DEBIT_CARD'));
