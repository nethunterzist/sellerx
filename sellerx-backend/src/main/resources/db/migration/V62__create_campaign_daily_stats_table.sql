-- V62: Create campaign_daily_stats table for granular campaign performance tracking

CREATE TABLE campaign_daily_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES trendyol_campaigns(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Date
    stat_date DATE NOT NULL,

    -- Performance metrics
    impressions BIGINT DEFAULT 0,
    clicks BIGINT DEFAULT 0,
    orders INTEGER DEFAULT 0,
    revenue DECIMAL(15, 2) DEFAULT 0,
    spend DECIMAL(15, 2) DEFAULT 0,

    -- Calculated metrics (stored for query performance)
    ctr DECIMAL(8, 4),   -- Click Through Rate: (clicks / impressions) * 100
    acos DECIMAL(8, 4),  -- Advertising Cost of Sales: (spend / revenue) * 100
    roas DECIMAL(8, 4),  -- Return on Ad Spend: revenue / spend
    cpc DECIMAL(10, 4),  -- Cost Per Click: spend / clicks
    cpo DECIMAL(10, 4),  -- Cost Per Order: spend / orders

    -- Raw data storage
    raw_data JSONB,

    -- Import tracking
    import_source VARCHAR(50) DEFAULT 'excel',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one record per campaign per day
    CONSTRAINT uq_campaign_daily_stat UNIQUE (campaign_id, stat_date)
);

-- Indexes for common queries
CREATE INDEX idx_daily_stats_campaign_id ON campaign_daily_stats(campaign_id);
CREATE INDEX idx_daily_stats_store_id ON campaign_daily_stats(store_id);
CREATE INDEX idx_daily_stats_date ON campaign_daily_stats(stat_date);
CREATE INDEX idx_daily_stats_campaign_date ON campaign_daily_stats(campaign_id, stat_date);
CREATE INDEX idx_daily_stats_store_date ON campaign_daily_stats(store_id, stat_date);
CREATE INDEX idx_daily_stats_store_date_range ON campaign_daily_stats(store_id, stat_date DESC);

-- Comments
COMMENT ON TABLE campaign_daily_stats IS 'Daily performance statistics for each campaign';
COMMENT ON COLUMN campaign_daily_stats.ctr IS 'Click Through Rate: (clicks / impressions) * 100';
COMMENT ON COLUMN campaign_daily_stats.acos IS 'Advertising Cost of Sales: (spend / revenue) * 100';
COMMENT ON COLUMN campaign_daily_stats.roas IS 'Return on Ad Spend: revenue / spend';
