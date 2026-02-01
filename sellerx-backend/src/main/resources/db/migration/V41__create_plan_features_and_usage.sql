-- Plan Features table
CREATE TABLE plan_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    feature_name VARCHAR(200) NOT NULL,
    feature_type VARCHAR(20) NOT NULL,
    limit_value INTEGER,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, feature_code)
);

-- Create indexes
CREATE INDEX idx_plan_features_plan_id ON plan_features(plan_id);
CREATE INDEX idx_plan_features_feature_code ON plan_features(feature_code);
CREATE INDEX idx_plan_features_enabled ON plan_features(is_enabled) WHERE is_enabled = true;

-- Add check constraint for feature type
ALTER TABLE plan_features ADD CONSTRAINT chk_feature_type
    CHECK (feature_type IN ('BOOLEAN', 'LIMIT', 'UNLIMITED'));

-- Feature Usage table
CREATE TABLE feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, feature_code, period_start)
);

-- Create indexes
CREATE INDEX idx_feature_usage_user_id ON feature_usage(user_id);
CREATE INDEX idx_feature_usage_feature_code ON feature_usage(feature_code);
CREATE INDEX idx_feature_usage_period ON feature_usage(period_start, period_end);

-- Insert default plan features
-- FREE plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Magaza', 'LIMIT', 1, true FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI QA Yanit', 'LIMIT', 10, true FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelismis Analitik', 'BOOLEAN', false FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhook_support', 'Webhook Destegi', 'BOOLEAN', false FROM subscription_plans WHERE code = 'FREE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erisimi', 'BOOLEAN', false FROM subscription_plans WHERE code = 'FREE';

-- STARTER plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Magaza', 'LIMIT', 3, true FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI QA Yanit', 'LIMIT', 100, true FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelismis Analitik', 'BOOLEAN', true FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhook_support', 'Webhook Destegi', 'BOOLEAN', false FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erisimi', 'BOOLEAN', false FROM subscription_plans WHERE code = 'STARTER';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Parasut Entegrasyonu', 'BOOLEAN', true FROM subscription_plans WHERE code = 'STARTER';

-- PRO plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'max_stores', 'Maksimum Magaza', 'LIMIT', 10, true FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, limit_value, is_enabled)
SELECT id, 'ai_qa_responses', 'AI QA Yanit', 'LIMIT', 500, true FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelismis Analitik', 'BOOLEAN', true FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhook_support', 'Webhook Destegi', 'BOOLEAN', true FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erisimi', 'BOOLEAN', true FROM subscription_plans WHERE code = 'PRO';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Parasut Entegrasyonu', 'BOOLEAN', true FROM subscription_plans WHERE code = 'PRO';

-- ENTERPRISE plan features
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'max_stores', 'Maksimum Magaza', 'UNLIMITED', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'ai_qa_responses', 'AI QA Yanit', 'UNLIMITED', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'advanced_analytics', 'Gelismis Analitik', 'BOOLEAN', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'webhook_support', 'Webhook Destegi', 'BOOLEAN', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'api_access', 'API Erisimi', 'BOOLEAN', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'parasut_integration', 'Parasut Entegrasyonu', 'BOOLEAN', true FROM subscription_plans WHERE code = 'ENTERPRISE';
INSERT INTO plan_features (plan_id, feature_code, feature_name, feature_type, is_enabled)
SELECT id, 'priority_support', 'Oncelikli Destek', 'BOOLEAN', true FROM subscription_plans WHERE code = 'ENTERPRISE';
