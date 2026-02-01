-- Fix "Yurt Dışı Operasyon Bedeli" to correct name "Yurtdışı Operasyon Iade Bedeli"
-- Trendyol API returns wrong transaction_type, but Excel export shows correct name
-- All 8 records with this type are actually refunds (İade)

UPDATE trendyol_deduction_invoices
SET transaction_type = 'Yurtdışı Operasyon Iade Bedeli'
WHERE transaction_type = 'Yurt Dışı Operasyon Bedeli';

-- Log how many records were updated
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count
    FROM trendyol_deduction_invoices
    WHERE transaction_type = 'Yurtdışı Operasyon Iade Bedeli';

    RAISE NOTICE 'Fixed % records: renamed to Yurtdışı Operasyon Iade Bedeli', updated_count;
END $$;
