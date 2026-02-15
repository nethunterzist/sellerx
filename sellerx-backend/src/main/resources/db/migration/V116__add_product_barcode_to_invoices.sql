-- Sandbox faturaları için ürün barkodu kolonu ekle
-- Bu sayede fatura silindiğinde ilgili ürünün komisyon/kargo değerleri sıfırlanabilir

ALTER TABLE trendyol_deduction_invoices
ADD COLUMN IF NOT EXISTS product_barcode VARCHAR(100);

COMMENT ON COLUMN trendyol_deduction_invoices.product_barcode IS
'Komisyon/Kargo faturasının ilişkili olduğu ürün barkodu (sandbox simülasyonu için)';
