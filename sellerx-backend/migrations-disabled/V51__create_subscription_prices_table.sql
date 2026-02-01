-- V35: Subscription Prices Table
-- Pricing tiers for each plan with billing cycle options

CREATE TABLE subscription_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    billing_cycle VARCHAR(20) NOT NULL,  -- MONTHLY, QUARTERLY, SEMIANNUAL
    price_amount DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(plan_id, billing_cycle)
);

-- Indexes
CREATE INDEX idx_subscription_prices_plan ON subscription_prices(plan_id);
CREATE INDEX idx_subscription_prices_active ON subscription_prices(is_active);

-- Seed prices (amounts in TRY)
-- FREE plan - only monthly, no charge
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 0, 0 FROM subscription_plans WHERE code = 'FREE';

-- STARTER plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 299.00, 0 FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 807.30, 10 FROM subscription_plans WHERE code = 'STARTER';  -- 299 * 3 * 0.9
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 1435.20, 20 FROM subscription_plans WHERE code = 'STARTER';  -- 299 * 6 * 0.8

-- PRO plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 599.00, 0 FROM subscription_plans WHERE code = 'PRO';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 1617.30, 10 FROM subscription_plans WHERE code = 'PRO';  -- 599 * 3 * 0.9
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 2875.20, 20 FROM subscription_plans WHERE code = 'PRO';  -- 599 * 6 * 0.8

-- ENTERPRISE plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 1499.00, 0 FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 4047.30, 10 FROM subscription_plans WHERE code = 'ENTERPRISE';  -- 1499 * 3 * 0.9
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 7195.20, 20 FROM subscription_plans WHERE code = 'ENTERPRISE';  -- 1499 * 6 * 0.8

COMMENT ON TABLE subscription_prices IS 'Pricing options for each subscription plan';
COMMENT ON COLUMN subscription_prices.billing_cycle IS 'MONTHLY, QUARTERLY (3 months), SEMIANNUAL (6 months)';
COMMENT ON COLUMN subscription_prices.discount_percentage IS 'Discount applied for longer billing cycles';
