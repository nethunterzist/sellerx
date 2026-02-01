-- V37: Payment Methods Table
-- Stored payment methods (iyzico card tokens)

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Payment method type
    type VARCHAR(20) NOT NULL DEFAULT 'CREDIT_CARD',  -- CREDIT_CARD, DEBIT_CARD

    -- iyzico card storage
    provider VARCHAR(50) NOT NULL DEFAULT 'iyzico',
    iyzico_card_user_key VARCHAR(255),  -- User key for card storage in iyzico
    iyzico_card_token VARCHAR(255) NOT NULL,  -- Stored card token

    -- Card display info (masked, for UI only)
    card_last_four VARCHAR(4) NOT NULL,
    card_brand VARCHAR(50),  -- VISA, MASTERCARD, TROY, AMEX
    card_family VARCHAR(100),  -- Card family/product name
    card_holder_name VARCHAR(255),
    card_exp_month INTEGER NOT NULL,
    card_exp_year INTEGER NOT NULL,
    card_bank_name VARCHAR(100),

    -- Flags
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_payment_methods_user ON payment_methods(user_id, is_active);
CREATE INDEX idx_payment_methods_token ON payment_methods(iyzico_card_token);

-- Ensure only one default payment method per user
CREATE UNIQUE INDEX idx_payment_methods_default
    ON payment_methods(user_id)
    WHERE is_default = TRUE AND is_active = TRUE;

COMMENT ON TABLE payment_methods IS 'Stored payment methods using iyzico tokenization';
COMMENT ON COLUMN payment_methods.iyzico_card_user_key IS 'iyzico user key for card storage';
COMMENT ON COLUMN payment_methods.iyzico_card_token IS 'Tokenized card reference, never store raw card data';
COMMENT ON COLUMN payment_methods.card_last_four IS 'Last 4 digits for display purposes only';
