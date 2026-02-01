-- V28: Create exchange_rates table for currency conversion
-- Stores daily exchange rates from TCMB (Turkish Central Bank)

CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'TCMB',
    fetched_at TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Ensure unique currency pairs
    CONSTRAINT uq_exchange_rate_pair UNIQUE (base_currency, target_currency)
);

-- Index for quick lookups by currency pair
CREATE INDEX IF NOT EXISTS idx_exchange_rates_currencies ON exchange_rates (base_currency, target_currency);

-- Index for checking rate validity
CREATE INDEX IF NOT EXISTS idx_exchange_rates_valid_until ON exchange_rates (valid_until);

-- Seed initial exchange rates (will be updated by scheduled job)
-- Using approximate rates as of January 2025
INSERT INTO exchange_rates (base_currency, target_currency, rate, source, fetched_at, valid_until)
VALUES
    ('TRY', 'USD', 0.029, 'TCMB', NOW(), NOW() + INTERVAL '1 day'),
    ('TRY', 'EUR', 0.027, 'TCMB', NOW(), NOW() + INTERVAL '1 day'),
    ('USD', 'TRY', 34.5, 'TCMB', NOW(), NOW() + INTERVAL '1 day'),
    ('EUR', 'TRY', 37.2, 'TCMB', NOW(), NOW() + INTERVAL '1 day')
ON CONFLICT (base_currency, target_currency) DO NOTHING;

COMMENT ON TABLE exchange_rates IS 'Daily exchange rates from TCMB for currency conversion (TRY/USD/EUR)';
