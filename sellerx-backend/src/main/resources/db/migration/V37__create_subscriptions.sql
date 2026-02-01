-- Subscriptions table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    price_id UUID NOT NULL REFERENCES subscription_prices(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    billing_cycle VARCHAR(20) NOT NULL,
    trial_start_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    grace_period_end TIMESTAMP,
    iyzico_subscription_reference VARCHAR(500),
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT false,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    downgrade_to_plan_id UUID REFERENCES subscription_plans(id),
    downgrade_to_price_id UUID REFERENCES subscription_prices(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_current_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscriptions_trial_end_date ON subscriptions(trial_end_date) WHERE trial_end_date IS NOT NULL;
CREATE INDEX idx_subscriptions_grace_period_end ON subscriptions(grace_period_end) WHERE grace_period_end IS NOT NULL;

-- Add check constraint for status
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_status
    CHECK (status IN ('PENDING_PAYMENT', 'TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'));

-- Add check constraint for billing cycle
ALTER TABLE subscriptions ADD CONSTRAINT chk_billing_cycle
    CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL'));
