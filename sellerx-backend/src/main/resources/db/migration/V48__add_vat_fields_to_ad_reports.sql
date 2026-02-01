-- V48__add_vat_fields_to_ad_reports.sql
-- Add VAT tracking fields to ad_reports for KDV Mahsuplasmasi

ALTER TABLE ad_reports ADD COLUMN IF NOT EXISTS ad_vat_rate INTEGER DEFAULT 20;
ALTER TABLE ad_reports ADD COLUMN IF NOT EXISTS ad_vat_amount DECIMAL(15,2);
ALTER TABLE ad_reports ADD COLUMN IF NOT EXISTS ad_net_spend DECIMAL(15,2);

-- Add comment for documentation
COMMENT ON COLUMN ad_reports.ad_vat_rate IS 'VAT rate for advertising (typically 20%)';
COMMENT ON COLUMN ad_reports.ad_vat_amount IS 'VAT amount from ad spend';
COMMENT ON COLUMN ad_reports.ad_net_spend IS 'Net ad spend after VAT (totalAdSpend - adVatAmount)';
