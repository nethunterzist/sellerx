-- Fix Komisyon Faturasi records: use trendyol_id as invoice_serial_number
-- Similar to V67 fix for Kargo Fatura, the id field contains the actual invoice serial number
-- for Komisyon Faturasi records where invoice_serial_number is empty

UPDATE trendyol_deduction_invoices
SET invoice_serial_number = trendyol_id
WHERE LOWER(transaction_type) LIKE '%komisyon%'
  AND LOWER(transaction_type) LIKE '%fatura%'
  AND (invoice_serial_number IS NULL OR invoice_serial_number = '');

-- Log how many records were updated (for visibility in logs)
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count
    FROM trendyol_deduction_invoices
    WHERE LOWER(transaction_type) LIKE '%komisyon%'
      AND LOWER(transaction_type) LIKE '%fatura%'
      AND invoice_serial_number = trendyol_id;

    RAISE NOTICE 'Fixed % Komisyon Faturasi records with invoice_serial_number', updated_count;
END $$;
