-- Cross-sell recommendation system for Q&A answers
-- Allows sellers to attach product recommendations to AI-generated answers

-- Main rules table
CREATE TABLE cross_sell_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,           -- KEYWORD, CATEGORY, PRODUCT, ALL_QUESTIONS
    trigger_conditions JSONB NOT NULL DEFAULT '{}', -- keywords[], categoryNames[], productBarcodes[]
    recommendation_type VARCHAR(50) NOT NULL,     -- SPECIFIC_PRODUCTS, SAME_CATEGORY, BESTSELLERS
    recommendation_text TEXT,                      -- Optional custom message for the recommendation
    priority INTEGER NOT NULL DEFAULT 0,          -- Higher = matched first
    max_products INTEGER NOT NULL DEFAULT 3,      -- Max products to recommend
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cross_sell_rules_store_active ON cross_sell_rules(store_id, active);
CREATE INDEX idx_cross_sell_rules_trigger ON cross_sell_rules(store_id, trigger_type, active);

-- Products linked to a cross-sell rule (for SPECIFIC_PRODUCTS type)
CREATE TABLE cross_sell_rule_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES cross_sell_rules(id) ON DELETE CASCADE,
    product_barcode VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cross_sell_rule_products_rule ON cross_sell_rule_products(rule_id);
CREATE UNIQUE INDEX idx_cross_sell_rule_products_unique ON cross_sell_rule_products(rule_id, product_barcode);

-- Per-store cross-sell settings
CREATE TABLE cross_sell_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_max_products INTEGER NOT NULL DEFAULT 3,
    include_in_answer BOOLEAN NOT NULL DEFAULT TRUE,  -- Append product links to AI answer text
    show_product_image BOOLEAN NOT NULL DEFAULT TRUE,
    show_product_price BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cross_sell_settings_store ON cross_sell_settings(store_id);

-- Analytics tracking for cross-sell recommendations
CREATE TABLE cross_sell_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    rule_id UUID REFERENCES cross_sell_rules(id) ON DELETE SET NULL,
    question_id UUID REFERENCES trendyol_questions(id) ON DELETE SET NULL,
    recommended_barcode VARCHAR(100) NOT NULL,
    was_included_in_answer BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cross_sell_analytics_store ON cross_sell_analytics(store_id);
CREATE INDEX idx_cross_sell_analytics_rule ON cross_sell_analytics(rule_id);
CREATE INDEX idx_cross_sell_analytics_created ON cross_sell_analytics(store_id, created_at);
