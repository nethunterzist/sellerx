-- V40: Plan Features, Feature Usage, and Subscription Events Tables
-- Feature gating system and subscription audit trail

-- Plan features table (detailed feature configuration per plan)
CREATE TABLE plan_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,

    feature_code VARCHAR(100) NOT NULL,
    feature_name VARCHAR(200) NOT NULL,

    -- Feature type
    feature_type VARCHAR(20) NOT NULL,  -- BOOLEAN, LIMIT, UNLIMITED

    -- Limit value (for LIMIT type features)
    limit_value INTEGER,

    -- Is feature enabled for this plan
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(plan_id, feature_code)
);

-- Feature usage tracking (for metered/limited features)
CREATE TABLE feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    feature_code VARCHAR(100) NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,

    -- Billing period this usage applies to
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, feature_code, period_start)
);

-- Subscription events (audit trail)
CREATE TABLE subscription_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- Event type
    event_type VARCHAR(50) NOT NULL,
    -- Events: CREATED, TRIAL_STARTED, TRIAL_ENDED, ACTIVATED, UPGRADED, DOWNGRADED,
    --         PAYMENT_FAILED, PAYMENT_SUCCEEDED, PAST_DUE, SUSPENDED, REACTIVATED,
    --         CANCELLED, EXPIRED, RENEWED

    -- State change tracking
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    previous_plan_id UUID REFERENCES subscription_plans(id),
    new_plan_id UUID REFERENCES subscription_plans(id),

    -- Additional event data
    metadata JSONB,
    -- Example: {"paymentId": "...", "amount": 599, "reason": "card_declined"}

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Payment webhook events (for idempotency)
CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,  -- iyzico event identifier

    event_type VARCHAR(50) NOT NULL,

    -- Related entities
    payment_id UUID REFERENCES payment_transactions(id),
    subscription_id UUID REFERENCES subscriptions(id),

    -- Raw payload
    payload TEXT,

    -- Processing status
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    -- Status: RECEIVED, PROCESSING, COMPLETED, FAILED, DUPLICATE

    error_message TEXT,
    processing_time_ms BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- Indexes for plan_features
CREATE INDEX idx_plan_features_plan ON plan_features(plan_id);
CREATE INDEX idx_plan_features_code ON plan_features(feature_code);

-- Indexes for feature_usage
CREATE INDEX idx_feature_usage_user ON feature_usage(user_id);
CREATE INDEX idx_feature_usage_period ON feature_usage(user_id, period_start, period_end);

-- Indexes for subscription_events
CREATE INDEX idx_subscription_events_subscription ON subscription_events(subscription_id);
CREATE INDEX idx_subscription_events_user ON subscription_events(user_id);
CREATE INDEX idx_subscription_events_type ON subscription_events(event_type);
CREATE INDEX idx_subscription_events_created ON subscription_events(created_at);

-- Indexes for payment_webhook_events
CREATE INDEX idx_payment_webhook_event_id ON payment_webhook_events(event_id);
CREATE INDEX idx_payment_webhook_status ON payment_webhook_events(processing_status);

-- Seed plan features
-- FREE plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Mağaza', 'LIMIT', 1, TRUE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_products_per_store', 'Mağaza Başı Ürün', 'LIMIT', 100, TRUE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI Yanıt Hakkı', 'LIMIT', 10, TRUE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'basic_dashboard', 'Temel Dashboard', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelişmiş Analitik', 'BOOLEAN', FALSE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhooks', 'Webhook Desteği', 'BOOLEAN', FALSE FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erişimi', 'BOOLEAN', FALSE FROM subscription_plans WHERE code = 'FREE';

-- STARTER plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Mağaza', 'LIMIT', 3, TRUE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_products_per_store', 'Mağaza Başı Ürün', 'LIMIT', 500, TRUE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI Yanıt Hakkı', 'LIMIT', 100, TRUE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'basic_dashboard', 'Temel Dashboard', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelişmiş Analitik', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhooks', 'Webhook Desteği', 'BOOLEAN', FALSE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erişimi', 'BOOLEAN', FALSE FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Paraşüt Entegrasyonu', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'STARTER';

-- PRO plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Mağaza', 'LIMIT', 10, TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'max_products_per_store', 'Mağaza Başı Ürün', 'UNLIMITED', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI Yanıt Hakkı', 'LIMIT', 500, TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'basic_dashboard', 'Temel Dashboard', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelişmiş Analitik', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhooks', 'Webhook Desteği', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erişimi', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Paraşüt Entegrasyonu', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'ai_suggestions', 'AI Önerileri', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'PRO';

-- ENTERPRISE plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'max_stores', 'Maksimum Mağaza', 'UNLIMITED', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'max_products_per_store', 'Mağaza Başı Ürün', 'UNLIMITED', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'ai_qa_responses', 'AI Yanıt Hakkı', 'UNLIMITED', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'basic_dashboard', 'Temel Dashboard', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelişmiş Analitik', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhooks', 'Webhook Desteği', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erişimi', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Paraşüt Entegrasyonu', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'ai_suggestions', 'AI Önerileri', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'priority_support', 'Öncelikli Destek', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'custom_integrations', 'Özel Entegrasyonlar', 'BOOLEAN', TRUE FROM subscription_plans WHERE code = 'ENTERPRISE';

COMMENT ON TABLE plan_features IS 'Detailed feature configuration per subscription plan';
COMMENT ON COLUMN plan_features.feature_type IS 'BOOLEAN (on/off), LIMIT (numeric cap), UNLIMITED (no limit)';
COMMENT ON TABLE feature_usage IS 'Metered feature usage tracking per billing period';
COMMENT ON TABLE subscription_events IS 'Audit trail for subscription lifecycle events';
COMMENT ON TABLE payment_webhook_events IS 'iyzico webhook events for idempotency';
