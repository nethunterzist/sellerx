-- Subscription Plans table
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_stores INTEGER,
    features JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for active plans lookup
CREATE INDEX idx_subscription_plans_active ON subscription_plans(is_active) WHERE is_active = true;
CREATE INDEX idx_subscription_plans_code ON subscription_plans(code);

-- Insert default plans
INSERT INTO subscription_plans (id, code, name, description, max_stores, features, sort_order) VALUES
    (gen_random_uuid(), 'FREE', 'Free', 'Ucretsiz plan - 1 magaza', 1,
     '{"advanced_analytics": false, "webhooks": false, "api_access": false, "priority_support": false, "parasut_integration": false}', 0),
    (gen_random_uuid(), 'STARTER', 'Starter', 'Baslangic plani - 3 magaza', 3,
     '{"advanced_analytics": true, "webhooks": false, "api_access": false, "priority_support": false, "parasut_integration": true}', 1),
    (gen_random_uuid(), 'PRO', 'Pro', 'Profesyonel plan - 10 magaza', 10,
     '{"advanced_analytics": true, "webhooks": true, "api_access": true, "priority_support": false, "parasut_integration": true}', 2),
    (gen_random_uuid(), 'ENTERPRISE', 'Enterprise', 'Kurumsal plan - Sinirsiz magaza', NULL,
     '{"advanced_analytics": true, "webhooks": true, "api_access": true, "priority_support": true, "parasut_integration": true}', 3);
