-- Create ad_reports table for advertising profitability analysis
CREATE TABLE IF NOT EXISTS ad_reports (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id UUID NOT NULL,

    -- Time period
    report_date DATE NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- Ad Spend Data (from Trendyol Excel)
    total_ad_spend DECIMAL(15,2) NOT NULL DEFAULT 0,
    ad_balance DECIMAL(15,2) DEFAULT 0,
    influencer_spend DECIMAL(15,2) DEFAULT 0,
    micro_influencer_spend DECIMAL(15,2) DEFAULT 0,

    -- Performance Metrics
    ad_impressions BIGINT DEFAULT 0,
    ad_clicks BIGINT DEFAULT 0,
    ad_orders INTEGER DEFAULT 0,
    ad_revenue DECIMAL(15,2) DEFAULT 0,

    -- Raw data storage for flexibility
    raw_data JSONB,

    -- Metadata
    import_source VARCHAR(50) DEFAULT 'excel',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    UNIQUE(store_id, report_date)
);

-- Create indexes for better query performance
CREATE INDEX idx_ad_reports_store_id ON ad_reports(store_id);
CREATE INDEX idx_ad_reports_report_date ON ad_reports(report_date DESC);
CREATE INDEX idx_ad_reports_store_date ON ad_reports(store_id, report_date DESC);
CREATE INDEX idx_ad_reports_period ON ad_reports(store_id, period_start, period_end);
