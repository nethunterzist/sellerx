-- V34: Subscription Plans Table
-- Base table for all subscription tiers

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_stores INTEGER,  -- NULL = unlimited
    features JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_subscription_plans_active ON subscription_plans(is_active, sort_order);
CREATE INDEX idx_subscription_plans_code ON subscription_plans(code);

-- Seed initial plans
INSERT INTO subscription_plans (code, name, description, max_stores, sort_order, features) VALUES
('FREE', 'Ücretsiz', 'Başlangıç için ideal, temel özellikler', 1, 1,
 '{"trialDays": 0, "analytics": false, "aiQa": false, "webhooks": false, "ads": false, "financial": false, "prioritySupport": false}'),
('STARTER', 'Starter', 'Küçük işletmeler için ideal', 3, 2,
 '{"trialDays": 14, "analytics": true, "aiQa": false, "webhooks": false, "ads": false, "financial": true, "prioritySupport": false}'),
('PRO', 'Pro', 'Büyüyen işletmeler için tüm özellikler', 10, 3,
 '{"trialDays": 14, "analytics": true, "aiQa": true, "webhooks": true, "ads": true, "financial": true, "prioritySupport": false}'),
('ENTERPRISE', 'Enterprise', 'Kurumsal çözümler ve öncelikli destek', NULL, 4,
 '{"trialDays": 14, "analytics": true, "aiQa": true, "webhooks": true, "ads": true, "financial": true, "prioritySupport": true, "customIntegrations": true}');

COMMENT ON TABLE subscription_plans IS 'Subscription plan definitions with feature flags';
COMMENT ON COLUMN subscription_plans.max_stores IS 'Maximum stores allowed, NULL means unlimited';
COMMENT ON COLUMN subscription_plans.features IS 'JSONB feature flags for plan capabilities';
