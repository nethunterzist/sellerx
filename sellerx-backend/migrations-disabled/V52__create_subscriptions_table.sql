-- V36: Subscriptions Table
-- User subscription records with lifecycle management

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    price_id UUID NOT NULL REFERENCES subscription_prices(id),

    -- Subscription lifecycle
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    -- Status values: PENDING_PAYMENT, TRIAL, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED, EXPIRED

    billing_cycle VARCHAR(20) NOT NULL,  -- MONTHLY, QUARTERLY, SEMIANNUAL

    -- Trial tracking
    trial_start_date TIMESTAMP,
    trial_end_date TIMESTAMP,

    -- Billing period
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,

    -- Grace period for failed payments
    grace_period_end TIMESTAMP,

    -- iyzico stored card reference
    iyzico_subscription_reference VARCHAR(255),

    -- Cancellation tracking
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,

    -- Auto-renewal
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,

    -- Plan change tracking (for downgrades scheduled at period end)
    downgrade_to_plan_id UUID REFERENCES subscription_plans(id),
    downgrade_to_price_id UUID REFERENCES subscription_prices(id),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- One active subscription per user
    UNIQUE(user_id)
);

-- Indexes for common queries
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_period_end ON subscriptions(current_period_end)
    WHERE status IN ('ACTIVE', 'TRIAL', 'PAST_DUE');
CREATE INDEX idx_subscriptions_grace_period ON subscriptions(grace_period_end)
    WHERE status = 'PAST_DUE';
CREATE INDEX idx_subscriptions_trial_end ON subscriptions(trial_end_date)
    WHERE status = 'TRIAL';

COMMENT ON TABLE subscriptions IS 'User subscription records with full lifecycle tracking';
COMMENT ON COLUMN subscriptions.status IS 'PENDING_PAYMENT, TRIAL, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED, EXPIRED';
COMMENT ON COLUMN subscriptions.grace_period_end IS '3-day grace period end for failed payment recovery';
COMMENT ON COLUMN subscriptions.cancel_at_period_end IS 'If true, subscription will not renew at period end';
