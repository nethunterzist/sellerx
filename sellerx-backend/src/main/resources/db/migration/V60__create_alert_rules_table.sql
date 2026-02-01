-- V60: Create Alert Rules System
-- Alert rules allow users to define custom notification triggers for stock, profit, orders, etc.

-- Enum-like alert types and conditions
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE,  -- NULL = all stores

    -- Rule definition
    name VARCHAR(200) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,      -- STOCK, PROFIT, PRICE, ORDER, SYSTEM
    condition_type VARCHAR(50) NOT NULL,   -- BELOW, ABOVE, EQUALS, CHANGED, ZERO
    threshold DECIMAL(15,2),               -- Threshold value (stock count, percentage, etc.)

    -- Optional scope filters
    product_barcode VARCHAR(100),          -- NULL = all products
    category_name VARCHAR(200),            -- NULL = all categories

    -- Notification channels
    email_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT false,
    in_app_enabled BOOLEAN DEFAULT true,

    -- Rule status
    active BOOLEAN DEFAULT true,

    -- Cooldown to prevent spam
    cooldown_minutes INTEGER DEFAULT 60,
    last_triggered_at TIMESTAMP,
    trigger_count INTEGER DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Alert history for tracking triggered alerts
CREATE TABLE alert_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID REFERENCES alert_rules(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id UUID REFERENCES stores(id) ON DELETE SET NULL,

    -- Alert details
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM',  -- LOW, MEDIUM, HIGH, CRITICAL

    -- Related data (product info, values, etc.)
    data JSONB,

    -- Delivery status
    email_sent BOOLEAN DEFAULT false,
    push_sent BOOLEAN DEFAULT false,
    in_app_sent BOOLEAN DEFAULT true,

    -- Read status
    read_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_alert_rules_user_active ON alert_rules(user_id, active);
CREATE INDEX idx_alert_rules_store ON alert_rules(store_id) WHERE store_id IS NOT NULL;
CREATE INDEX idx_alert_rules_type ON alert_rules(alert_type, active);

CREATE INDEX idx_alert_history_user_created ON alert_history(user_id, created_at DESC);
CREATE INDEX idx_alert_history_user_unread ON alert_history(user_id, read_at) WHERE read_at IS NULL;
CREATE INDEX idx_alert_history_store ON alert_history(store_id, created_at DESC) WHERE store_id IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE alert_rules IS 'User-defined alert rules for monitoring stock, profit, orders, etc.';
COMMENT ON TABLE alert_history IS 'History of triggered alerts for user notification center';
COMMENT ON COLUMN alert_rules.alert_type IS 'Type: STOCK, PROFIT, PRICE, ORDER, SYSTEM';
COMMENT ON COLUMN alert_rules.condition_type IS 'Condition: BELOW, ABOVE, EQUALS, CHANGED, ZERO';
COMMENT ON COLUMN alert_rules.cooldown_minutes IS 'Minimum minutes between repeat triggers to prevent spam';
