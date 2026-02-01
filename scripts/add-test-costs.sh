#!/bin/bash

# Tüm ürünlere 200 TL test maliyeti ekle
# Kullanım: ./scripts/add-test-costs.sh

echo "🔧 Tüm ürünlere 200 TL maliyet ekleniyor..."

docker exec -i sellerx-db psql -U postgres -d sellerx_db << 'EOF'

-- Önce mevcut durumu göster
SELECT
    '📊 Mevcut Durum:' as bilgi,
    COUNT(*) as toplam_urun,
    COUNT(*) FILTER (WHERE jsonb_array_length(cost_and_stock_info) > 0) as maliyetli,
    COUNT(*) FILTER (WHERE jsonb_array_length(cost_and_stock_info) = 0) as maliyetsiz
FROM trendyol_products;

-- Tüm ürünlere 200 TL maliyet ekle
UPDATE trendyol_products
SET cost_and_stock_info = jsonb_build_array(
    jsonb_build_object(
        'quantity', GREATEST(COALESCE(trendyol_quantity, 0), 100),
        'unitCost', 200.0,
        'costVatRate', 20,
        'stockDate', CURRENT_DATE::text,
        'usedQuantity', 0
    )
),
updated_at = NOW();

-- Sonucu göster
SELECT
    '✅ Güncelleme Tamamlandı:' as bilgi,
    COUNT(*) as guncellenen_urun
FROM trendyol_products
WHERE jsonb_array_length(cost_and_stock_info) > 0;

-- Örnek ürünleri göster
SELECT
    LEFT(title, 40) as urun_adi,
    barcode,
    trendyol_quantity as stok,
    sale_price as satis,
    (cost_and_stock_info->0->>'unitCost')::numeric as maliyet
FROM trendyol_products
LIMIT 10;

EOF

echo ""
echo "✅ Tamamlandı! Artık dashboard'da kâr/zarar hesaplamaları görünecek."
