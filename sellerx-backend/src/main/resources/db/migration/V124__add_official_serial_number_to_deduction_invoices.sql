-- Add official serial number column for storing the original invoice serial from Trendyol
ALTER TABLE trendyol_deduction_invoices ADD COLUMN official_serial_number VARCHAR(100);

-- Backfill from existing data where invoice_serial_number matches official format
UPDATE trendyol_deduction_invoices
SET official_serial_number = invoice_serial_number
WHERE invoice_serial_number ~ '^(DDA|DDF|AZD|TYE|TEX)[A-Z0-9]';

-- Partial index for efficient lookups by store and official serial number
CREATE INDEX idx_deduction_invoices_official_serial
    ON trendyol_deduction_invoices(store_id, official_serial_number)
    WHERE official_serial_number IS NOT NULL;
