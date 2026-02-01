-- Tüm ürünlere 200 TL maliyet ekle
-- Kullanım: ./db.sh connect sonra \i /scripts/bulk-add-cost.sql
-- Veya: docker exec -i sellerx-db psql -U postgres -d sellerx_db < scripts/bulk-add-cost.sql

-- Mevcut maliyet sayısını göster
SELECT
    COUNT(*) as toplam_urun,
    COUNT(*) FILTER (WHERE jsonb_array_length(cost_and_stock_info) > 0) as maliyetli_urun,
    COUNT(*) FILTER (WHERE jsonb_array_length(cost_and_stock_info) = 0) as maliyetsiz_urun
FROM trendyol_products;

-- Tüm ürünlere 200 TL maliyet ekle
-- Her ürünün mevcut stok miktarı (trendyol_quantity) kadar, 200 TL birim maliyetle
UPDATE trendyol_products
SET cost_and_stock_info = jsonb_build_array(
    jsonb_build_object(
        'quantity', COALESCE(trendyol_quantity, 100),
        'unitCost', 200.0,
        'costVatRate', 20,
        'stockDate', CURRENT_DATE::text,
        'usedQuantity', 0
    )
),
updated_at = NOW()
WHERE store_id IN (
    SELECT id FROM stores WHERE store_name ILIKE '%k-pure%' OR store_name ILIKE '%test%'
);

-- Sonucu göster
SELECT
    p.title,
    p.barcode,
    p.trendyol_quantity as stok,
    p.sale_price as satis_fiyati,
    (cost_and_stock_info->0->>'unitCost')::numeric as maliyet,
    p.sale_price - 200 as brut_kar_tahmini
FROM trendyol_products p
WHERE jsonb_array_length(cost_and_stock_info) > 0
ORDER BY p.title
LIMIT 20;

-- Toplam güncellenen ürün sayısı
SELECT COUNT(*) as guncellenen_urun_sayisi
FROM trendyol_products
WHERE jsonb_array_length(cost_and_stock_info) > 0;
