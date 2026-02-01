-- Payment Methods (iyzico stored cards)
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Payment method type
    type VARCHAR(20) NOT NULL DEFAULT 'CREDIT_CARD',

    -- iyzico tokens
    iyzico_card_user_key VARCHAR(500),
    iyzico_card_token VARCHAR(500),

    -- Card info (masked/safe to store)
    card_last_four VARCHAR(4),
    card_brand VARCHAR(20),
    card_family VARCHAR(50),
    card_holder_name VARCHAR(100),
    card_exp_month INTEGER,
    card_exp_year INTEGER,
    card_bank_name VARCHAR(100),

    -- Status
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_methods_user ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(user_id, is_default) WHERE is_default = true;
CREATE INDEX idx_payment_methods_active ON payment_methods(user_id, is_active) WHERE is_active = true;
CREATE UNIQUE INDEX idx_payment_methods_token ON payment_methods(user_id, iyzico_card_token) WHERE is_active = true;
