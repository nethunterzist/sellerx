-- Subscription Prices
CREATE TABLE subscription_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    billing_cycle VARCHAR(20) NOT NULL,
    price_amount DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'TRY',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, billing_cycle)
);

-- Insert default prices
-- FREE plan (all cycles same price: 0)
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 0, 0 FROM subscription_plans WHERE code = 'FREE';

-- STARTER plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 299, 0 FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 807, 10 FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 1435, 20 FROM subscription_plans WHERE code = 'STARTER';

-- PRO plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 599, 0 FROM subscription_plans WHERE code = 'PRO';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 1617, 10 FROM subscription_plans WHERE code = 'PRO';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 2875, 20 FROM subscription_plans WHERE code = 'PRO';

-- ENTERPRISE plan
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'MONTHLY', 1499, 0 FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'QUARTERLY', 4047, 10 FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO subscription_prices (plan_id, billing_cycle, price_amount, discount_percentage)
SELECT id, 'SEMIANNUAL', 7195, 20 FROM subscription_plans WHERE code = 'ENTERPRISE';

CREATE INDEX idx_subscription_prices_plan ON subscription_prices(plan_id);
CREATE INDEX idx_subscription_prices_cycle ON subscription_prices(billing_cycle);
