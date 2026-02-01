-- Fix Platform Hizmet Bedeli records: use trendyol_id as invoice_serial_number
-- Similar to Komisyon Faturası fix, the id field contains the actual invoice serial number
-- for Platform Hizmet Bedeli records where invoice_serial_number is empty

UPDATE trendyol_deduction_invoices
SET invoice_serial_number = trendyol_id
WHERE LOWER(transaction_type) LIKE '%platform%hizmet%'
  AND (invoice_serial_number IS NULL OR invoice_serial_number = '');

-- Log how many records were updated
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count
    FROM trendyol_deduction_invoices
    WHERE LOWER(transaction_type) LIKE '%platform%hizmet%'
      AND invoice_serial_number = trendyol_id;

    RAISE NOTICE 'Fixed % Platform Hizmet Bedeli records with invoice_serial_number', updated_count;
END $$;
