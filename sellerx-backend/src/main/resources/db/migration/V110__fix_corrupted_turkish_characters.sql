-- V110: Fix corrupted Turkish characters in transaction_type
-- Problem: Trendyol API started returning corrupted characters after Dec 30, 2025 update
-- First affected records appeared on Jan 21, 2026
-- Affected types: AZ-Yurtdışı Operasyon, Tedarik Edememe, Yanlış/Eksik/Kusurlu Ürün faturaları

UPDATE trendyol_deduction_invoices
SET transaction_type = CASE
    WHEN transaction_type = 'AZ-YURTDÕ_Õ OPERASYON BEDELI %18'
        THEN 'AZ-Yurtdışı Operasyon Bedeli %18'
    WHEN transaction_type = 'TEDARIK EDEMEME FATURASI'
        THEN 'Tedarik Edememe'
    WHEN transaction_type = 'YANLIS URUN FATURASI'
        THEN 'Yanlış Ürün Faturası'
    WHEN transaction_type = 'EKSIK URUN FATURASI'
        THEN 'Eksik Ürün Faturası'
    WHEN transaction_type = 'KUSURLU URUN FATURASI'
        THEN 'Kusurlu Ürün Faturası'
    ELSE transaction_type
END
WHERE transaction_type IN (
    'AZ-YURTDÕ_Õ OPERASYON BEDELI %18',
    'TEDARIK EDEMEME FATURASI',
    'YANLIS URUN FATURASI',
    'EKSIK URUN FATURASI',
    'KUSURLU URUN FATURASI'
);

-- Log how many records were fixed
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'V110: Fixed % records with corrupted Turkish characters', updated_count;
END $$;
