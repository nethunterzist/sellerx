-- V108: Remove duplicate deduction invoices
-- Problem: Trendyol API sometimes returns same invoice with different ID formats
-- Example: First sync returns id=123456789, later sync returns id=DDF2025015689931
-- Both refer to same invoice but unique constraint (store_id, trendyol_id) doesn't catch it

-- Delete numeric-only ID entries that have a matching DDF/AZD/DCF/TYE format entry
-- Keep the actual invoice serial number (alphanumeric), delete the numeric placeholder
DELETE FROM trendyol_deduction_invoices tdi
WHERE tdi.invoice_serial_number ~ '^[0-9]+$'
AND EXISTS (
    SELECT 1 FROM trendyol_deduction_invoices d2
    WHERE d2.store_id = tdi.store_id
    AND d2.debt = tdi.debt
    AND d2.transaction_date = tdi.transaction_date
    AND d2.transaction_type = tdi.transaction_type
    AND d2.invoice_serial_number ~ '^[A-Z]'
    AND d2.id != tdi.id
);

-- Log how many duplicates were removed (for audit purposes)
DO $$
DECLARE
    deleted_count INTEGER;
BEGIN
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'V108: Removed % duplicate invoice entries with numeric IDs', deleted_count;
END $$;
