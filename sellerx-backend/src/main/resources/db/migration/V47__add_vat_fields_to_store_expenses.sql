-- V47__add_vat_fields_to_store_expenses.sql
-- Add VAT tracking fields to store_expenses for KDV Mahsuplasmasi

ALTER TABLE store_expenses ADD COLUMN IF NOT EXISTS vat_rate INTEGER;
ALTER TABLE store_expenses ADD COLUMN IF NOT EXISTS vat_amount DECIMAL(10,2);
ALTER TABLE store_expenses ADD COLUMN IF NOT EXISTS is_vat_deductible BOOLEAN DEFAULT true;
ALTER TABLE store_expenses ADD COLUMN IF NOT EXISTS net_amount DECIMAL(10,2);

-- Add comment for documentation
COMMENT ON COLUMN store_expenses.vat_rate IS 'VAT rate: 0, 1, 10, or 20 percent';
COMMENT ON COLUMN store_expenses.vat_amount IS 'VAT amount calculated from the expense';
COMMENT ON COLUMN store_expenses.is_vat_deductible IS 'Whether this expense VAT can be deducted';
COMMENT ON COLUMN store_expenses.net_amount IS 'Net amount after VAT (amount - vat_amount)';
