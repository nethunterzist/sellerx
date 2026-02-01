-- Subscription events audit trail table
CREATE TABLE subscription_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    previous_plan_id UUID REFERENCES subscription_plans(id),
    new_plan_id UUID REFERENCES subscription_plans(id),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_subscription_events_subscription_id ON subscription_events(subscription_id);
CREATE INDEX idx_subscription_events_user_id ON subscription_events(user_id);
CREATE INDEX idx_subscription_events_event_type ON subscription_events(event_type);
CREATE INDEX idx_subscription_events_created_at ON subscription_events(created_at);

-- Add check constraint for event type
ALTER TABLE subscription_events ADD CONSTRAINT chk_event_type
    CHECK (event_type IN ('CREATED', 'ACTIVATED', 'CANCELLED', 'REACTIVATED', 'UPGRADED', 'DOWNGRADED',
                          'TRIAL_STARTED', 'TRIAL_ENDED', 'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED',
                          'RENEWED', 'SUSPENDED', 'RESUMED', 'EXPIRED'));
