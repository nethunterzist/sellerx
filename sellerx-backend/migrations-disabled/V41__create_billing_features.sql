-- Plan Features (detailed feature configuration)
CREATE TABLE plan_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    feature_code VARCHAR(100) NOT NULL,
    feature_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    limit_value INTEGER,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, feature_code)
);

-- Feature Usage Tracking
CREATE TABLE feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    feature_code VARCHAR(100) NOT NULL,
    usage_count INTEGER DEFAULT 0,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, feature_code, period_start)
);

-- Subscription Events (audit trail)
CREATE TABLE subscription_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Webhook Events (idempotency)
CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(200) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    payment_id UUID REFERENCES payment_transactions(id),
    subscription_id UUID REFERENCES subscriptions(id),
    payload TEXT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    processing_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Insert plan features
INSERT INTO plan_features (plan_id, feature_code, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'LIMIT', 1, true FROM subscription_plans WHERE code = 'FREE'
UNION ALL
SELECT id, 'advanced_analytics', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'FREE'
UNION ALL
SELECT id, 'ai_qa_responses', 'LIMIT', 10, true FROM subscription_plans WHERE code = 'FREE'
UNION ALL
SELECT id, 'webhook_support', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'FREE'
UNION ALL
SELECT id, 'api_access', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'FREE'
UNION ALL
SELECT id, 'parasut_integration', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'FREE';

INSERT INTO plan_features (plan_id, feature_code, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'LIMIT', 3, true FROM subscription_plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'advanced_analytics', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'ai_qa_responses', 'LIMIT', 100, true FROM subscription_plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'webhook_support', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'api_access', 'BOOLEAN', NULL, false FROM subscription_plans WHERE code = 'STARTER'
UNION ALL
SELECT id, 'parasut_integration', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'STARTER';

INSERT INTO plan_features (plan_id, feature_code, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'LIMIT', 10, true FROM subscription_plans WHERE code = 'PRO'
UNION ALL
SELECT id, 'advanced_analytics', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'PRO'
UNION ALL
SELECT id, 'ai_qa_responses', 'LIMIT', 500, true FROM subscription_plans WHERE code = 'PRO'
UNION ALL
SELECT id, 'webhook_support', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'PRO'
UNION ALL
SELECT id, 'api_access', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'PRO'
UNION ALL
SELECT id, 'parasut_integration', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'PRO';

INSERT INTO plan_features (plan_id, feature_code, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'UNLIMITED', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'advanced_analytics', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'ai_qa_responses', 'UNLIMITED', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'webhook_support', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'api_access', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE'
UNION ALL
SELECT id, 'parasut_integration', 'BOOLEAN', NULL, true FROM subscription_plans WHERE code = 'ENTERPRISE';

CREATE INDEX idx_plan_features_plan ON plan_features(plan_id);
CREATE INDEX idx_plan_features_code ON plan_features(feature_code);

CREATE INDEX idx_feature_usage_user ON feature_usage(user_id);
CREATE INDEX idx_feature_usage_code ON feature_usage(feature_code);
CREATE INDEX idx_feature_usage_period ON feature_usage(period_start, period_end);

CREATE INDEX idx_subscription_events_subscription ON subscription_events(subscription_id);
CREATE INDEX idx_subscription_events_type ON subscription_events(event_type);
CREATE INDEX idx_subscription_events_created ON subscription_events(created_at);

CREATE INDEX idx_payment_webhook_events_event ON payment_webhook_events(event_id);
CREATE INDEX idx_payment_webhook_events_status ON payment_webhook_events(processing_status);
