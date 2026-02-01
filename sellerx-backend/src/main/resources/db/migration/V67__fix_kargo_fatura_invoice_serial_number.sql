-- Fix: Update existing "Kargo Fatura" records to set invoiceSerialNumber = trendyolId
-- According to Trendyol API documentation, for "Kargo Fatura" records,
-- the 'id' field (stored as trendyol_id) IS the invoiceSerialNumber
-- that should be used to call the cargo invoice detail API.

UPDATE trendyol_deduction_invoices
SET invoice_serial_number = trendyol_id
WHERE (transaction_type = 'Kargo Fatura' OR transaction_type = 'Kargo Faturası')
  AND invoice_serial_number IS NULL
  AND trendyol_id IS NOT NULL;

-- Log the update count
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count
    FROM trendyol_deduction_invoices
    WHERE (transaction_type = 'Kargo Fatura' OR transaction_type = 'Kargo Faturası')
      AND invoice_serial_number IS NOT NULL;
    RAISE NOTICE 'Updated % Kargo Fatura records with invoice_serial_number', updated_count;
END $$;
