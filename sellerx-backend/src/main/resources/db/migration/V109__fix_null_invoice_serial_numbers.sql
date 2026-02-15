-- V109: Fix NULL or empty invoice_serial_number values
-- Problem: Some invoice types (Eksik Ürün Faturası, Yanlış Ürün Faturası)
-- don't have fallback logic in TrendyolOtherFinancialsService.java
-- When API doesn't return invoiceSerialNumber, it stays NULL

-- Use trendyol_id as fallback for invoice_serial_number
UPDATE trendyol_deduction_invoices
SET invoice_serial_number = trendyol_id
WHERE invoice_serial_number IS NULL OR invoice_serial_number = '';

-- Log how many records were fixed
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'V109: Fixed % records with NULL/empty invoice_serial_number', updated_count;
END $$;
