-- Subscription Plans
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_stores INTEGER,
    features JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default plans
INSERT INTO subscription_plans (code, name, description, max_stores, features, sort_order) VALUES
('FREE', 'Free', 'Başlangıç için ücretsiz plan', 1,
 '{"advanced_analytics": false, "ai_qa_responses": 10, "webhook_support": false, "api_access": false, "priority_support": false, "parasut_integration": false}',
 1),
('STARTER', 'Starter', 'Küçük işletmeler için ideal', 3,
 '{"advanced_analytics": true, "ai_qa_responses": 100, "webhook_support": false, "api_access": false, "priority_support": false, "parasut_integration": true}',
 2),
('PRO', 'Pro', 'Büyüyen işletmeler için', 10,
 '{"advanced_analytics": true, "ai_qa_responses": 500, "webhook_support": true, "api_access": true, "priority_support": false, "parasut_integration": true}',
 3),
('ENTERPRISE', 'Enterprise', 'Sınırsız erişim ve öncelikli destek', NULL,
 '{"advanced_analytics": true, "ai_qa_responses": -1, "webhook_support": true, "api_access": true, "priority_support": true, "parasut_integration": true}',
 4);

CREATE INDEX idx_subscription_plans_code ON subscription_plans(code);
CREATE INDEX idx_subscription_plans_active ON subscription_plans(is_active);
