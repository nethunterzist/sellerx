-- V63: Add campaign_id reference to ad_reports table
-- This links existing ad reports to specific campaigns

ALTER TABLE ad_reports
ADD COLUMN campaign_id UUID REFERENCES trendyol_campaigns(id) ON DELETE SET NULL;

-- Index for campaign lookups
CREATE INDEX idx_ad_reports_campaign_id ON ad_reports(campaign_id);

-- Comment
COMMENT ON COLUMN ad_reports.campaign_id IS 'Optional link to specific campaign - allows campaign-level reporting';
