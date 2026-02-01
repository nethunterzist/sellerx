-- V61: Create trendyol_campaigns table for campaign-level ad tracking
-- Trendyol does not have a public Ads API, so this supports Excel import

CREATE TYPE campaign_type AS ENUM ('PRODUCT', 'STORE', 'INFLUENCER', 'MICRO_INFLUENCER', 'OTHER');
CREATE TYPE campaign_status AS ENUM ('ACTIVE', 'PAUSED', 'COMPLETED', 'DRAFT', 'ARCHIVED');

CREATE TABLE trendyol_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Trendyol identifiers (nullable - may not exist if imported from Excel)
    campaign_id VARCHAR(100),
    campaign_name VARCHAR(255) NOT NULL,
    campaign_type campaign_type NOT NULL DEFAULT 'PRODUCT',
    status campaign_status NOT NULL DEFAULT 'ACTIVE',

    -- Budget & Spending
    daily_budget DECIMAL(15, 2),
    total_budget DECIMAL(15, 2),
    spent_amount DECIMAL(15, 2) DEFAULT 0,
    remaining_budget DECIMAL(15, 2),

    -- Date range
    start_date DATE,
    end_date DATE,

    -- Aggregated metrics (updated from daily stats)
    total_impressions BIGINT DEFAULT 0,
    total_clicks BIGINT DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    total_revenue DECIMAL(15, 2) DEFAULT 0,
    total_spend DECIMAL(15, 2) DEFAULT 0,

    -- Calculated metrics
    avg_ctr DECIMAL(8, 4),  -- Click Through Rate
    avg_acos DECIMAL(8, 4), -- Advertising Cost of Sales
    avg_roas DECIMAL(8, 4), -- Return on Ad Spend

    -- Raw data storage for flexibility
    raw_data JSONB,

    -- Import tracking
    import_source VARCHAR(50) DEFAULT 'excel',
    last_synced_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint per store + campaign_id (if exists) or campaign_name
    CONSTRAINT uq_store_campaign UNIQUE (store_id, campaign_id)
);

-- Indexes for common queries
CREATE INDEX idx_campaigns_store_id ON trendyol_campaigns(store_id);
CREATE INDEX idx_campaigns_status ON trendyol_campaigns(status);
CREATE INDEX idx_campaigns_type ON trendyol_campaigns(campaign_type);
CREATE INDEX idx_campaigns_dates ON trendyol_campaigns(start_date, end_date);
CREATE INDEX idx_campaigns_store_status ON trendyol_campaigns(store_id, status);

-- Comments
COMMENT ON TABLE trendyol_campaigns IS 'Trendyol advertising campaigns - supports Excel import and future API integration';
COMMENT ON COLUMN trendyol_campaigns.campaign_id IS 'Trendyol campaign ID - nullable for Excel imports';
COMMENT ON COLUMN trendyol_campaigns.import_source IS 'Source of data: excel, api, manual';
