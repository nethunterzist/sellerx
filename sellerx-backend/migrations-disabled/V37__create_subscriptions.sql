-- Subscriptions
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    price_id UUID REFERENCES subscription_prices(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',

    -- Trial period
    trial_start_date TIMESTAMP,
    trial_end_date TIMESTAMP,

    -- Current billing period
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,

    -- Grace period for failed payments
    grace_period_end TIMESTAMP,

    -- Cancellation
    cancelled_at TIMESTAMP,
    cancel_reason TEXT,
    cancel_at_period_end BOOLEAN DEFAULT false,

    -- Renewal
    auto_renew BOOLEAN DEFAULT true,

    -- Metadata
    metadata JSONB DEFAULT '{}',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(user_id)
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_trial_end ON subscriptions(trial_end_date) WHERE trial_end_date IS NOT NULL;
CREATE INDEX idx_subscriptions_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscriptions_grace_end ON subscriptions(grace_period_end) WHERE grace_period_end IS NOT NULL;
