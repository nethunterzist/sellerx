-- Subscription Prices table
CREATE TABLE subscription_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    billing_cycle VARCHAR(20) NOT NULL,
    price_amount DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, billing_cycle)
);

-- Create indexes
CREATE INDEX idx_subscription_prices_plan_id ON subscription_prices(plan_id);
CREATE INDEX idx_subscription_prices_active ON subscription_prices(is_active) WHERE is_active = true;

-- Insert prices for each plan
-- FREE plan has no prices (it's free)

-- STARTER prices
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 299.00, 0 FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 807.30, 10 FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 1435.20, 20 FROM subscription_plans WHERE code = 'STARTER';

-- PRO prices
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 599.00, 0 FROM subscription_plans WHERE code = 'PRO';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 1617.30, 10 FROM subscription_plans WHERE code = 'PRO';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 2875.20, 20 FROM subscription_plans WHERE code = 'PRO';

-- ENTERPRISE prices
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 1499.00, 0 FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 4047.30, 10 FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 7195.20, 20 FROM subscription_plans WHERE code = 'ENTERPRISE';
