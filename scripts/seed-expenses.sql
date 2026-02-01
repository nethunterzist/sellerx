-- =====================================================
-- SellerX Demo Gider Verileri - Trendyol Satıcısı
-- Tarih Aralığı: Ağustos 2025 - Ocak 2026 (6 ay)
-- Mağaza: k-pure (test@test.com kullanıcısı)
-- =====================================================

-- Temizlik (opsiyonel - mevcut demo verileri silmek için)
-- DELETE FROM store_expenses WHERE store_id = '0824e453-3761-4cdf-96c9-3215a7770618';

DO $$
DECLARE
    v_store_id UUID := '0824e453-3761-4cdf-96c9-3215a7770618';
    v_cat_ambalaj UUID;
    v_cat_kargo UUID;
    v_cat_reklam UUID;
    v_cat_ofis UUID;
    v_cat_muhasebe UUID;
    v_cat_diger UUID;
    v_month DATE;
    v_week DATE;
BEGIN
    -- Kategori ID'lerini al
    SELECT id INTO v_cat_ambalaj FROM expense_categories WHERE name = 'Ambalaj';
    SELECT id INTO v_cat_kargo FROM expense_categories WHERE name = 'Kargo';
    SELECT id INTO v_cat_reklam FROM expense_categories WHERE name = 'Reklam';
    SELECT id INTO v_cat_ofis FROM expense_categories WHERE name = 'Ofis';
    SELECT id INTO v_cat_muhasebe FROM expense_categories WHERE name = 'Muhasebe';
    SELECT id INTO v_cat_diger FROM expense_categories WHERE name = 'Diğer';

    -- =========================================================
    -- AYLIK (MONTHLY) GİDERLER - 6 ay boyunca tekrar (Ağu-Oca)
    -- =========================================================
    FOR v_month IN
        SELECT generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval)
    LOOP
        -- === AMBALAJ ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ambalaj, v_store_id, NULL, v_month + interval '0 days', 'MONTHLY', 'Koli ve bant alımı', 2500.00, 20, 500.00, true, 2500.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ambalaj, v_store_id, NULL, v_month + interval '2 days', 'MONTHLY', 'Bubble wrap (patpat)', 800.00, 20, 160.00, true, 800.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ambalaj, v_store_id, NULL, v_month + interval '4 days', 'MONTHLY', 'Etiket ve barkod basımı', 350.00, 20, 70.00, true, 350.00, v_month, v_month);

        -- === KARGO ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_kargo, v_store_id, NULL, v_month + interval '5 days', 'MONTHLY', 'Trendyol kargo farkı', 4500.00, 20, 900.00, true, 4500.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_kargo, v_store_id, NULL, v_month + interval '10 days', 'MONTHLY', 'Ürün iade kargo bedeli', 1800.00, 20, 360.00, true, 1800.00, v_month, v_month);

        -- === REKLAM ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_reklam, v_store_id, NULL, v_month + interval '1 day', 'MONTHLY', 'Trendyol reklam bütçesi', 8000.00, 20, 1600.00, true, 8000.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_reklam, v_store_id, NULL, v_month + interval '3 days', 'MONTHLY', 'Instagram/Meta reklam', 3500.00, 20, 700.00, true, 3500.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_reklam, v_store_id, NULL, v_month + interval '3 days', 'MONTHLY', 'Google Ads kampanyası', 2000.00, 20, 400.00, true, 2000.00, v_month, v_month);

        -- === OFİS ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ofis, v_store_id, NULL, v_month + interval '0 days', 'MONTHLY', 'Depo/ofis kirası', 15000.00, 20, 3000.00, true, 15000.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ofis, v_store_id, NULL, v_month + interval '15 days', 'MONTHLY', 'Elektrik ve su faturası', 2200.00, 20, 440.00, true, 2200.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ofis, v_store_id, NULL, v_month + interval '20 days', 'MONTHLY', 'İnternet ve telefon', 900.00, 20, 180.00, true, 900.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_ofis, v_store_id, NULL, v_month + interval '12 days', 'MONTHLY', 'Ofis malzemesi alımı', 450.00, 20, 90.00, true, 450.00, v_month, v_month);

        -- === MUHASEBE ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_muhasebe, v_store_id, NULL, v_month + interval '0 days', 'MONTHLY', 'Mali müşavir ücreti', 4000.00, 20, 800.00, true, 4000.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_muhasebe, v_store_id, NULL, v_month + interval '1 day', 'MONTHLY', 'e-Fatura yazılımı aboneliği', 350.00, 20, 70.00, true, 350.00, v_month, v_month);

        -- === DİĞER ===
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_diger, v_store_id, NULL, v_month + interval '0 days', 'MONTHLY', 'Personel maaşı (depocu)', 22000.00, 0, 0.00, false, 22000.00, v_month, v_month);

        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_diger, v_store_id, NULL, v_month + interval '1 day', 'MONTHLY', 'Trendyol mağaza danışmanlığı', 2500.00, 20, 500.00, true, 2500.00, v_month, v_month);

    END LOOP;

    -- =========================================================
    -- HAFTALIK (WEEKLY) GİDERLER - ~26 hafta (Ağu-Oca)
    -- =========================================================
    FOR v_week IN
        SELECT generate_series('2025-08-04'::date, '2026-01-26'::date, '1 week'::interval)
    LOOP
        -- Kargo: Kurye hizmeti (acil teslimat) - her Pazartesi
        INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
        VALUES (gen_random_uuid(), v_cat_kargo, v_store_id, NULL, v_week, 'WEEKLY', 'Kurye hizmeti (acil teslimat)', 600.00, 20, 120.00, true, 600.00, v_week, v_week);
    END LOOP;

    -- =========================================================
    -- YILLIK (YEARLY) GİDERLER - tek kayıt
    -- =========================================================

    -- Ambalaj makinesi bakımı - Ekim 2025
    INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
    VALUES (gen_random_uuid(), v_cat_ambalaj, v_store_id, NULL, '2025-10-15'::timestamp, 'YEARLY', 'Ambalaj makinesi bakımı', 1200.00, 20, 240.00, true, 1200.00, '2025-10-15', '2025-10-15');

    -- Yıllık vergi danışmanlığı - Ocak 2026
    INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
    VALUES (gen_random_uuid(), v_cat_muhasebe, v_store_id, NULL, '2026-01-10'::timestamp, 'YEARLY', 'Yıllık vergi danışmanlığı', 8000.00, 20, 1600.00, true, 8000.00, '2026-01-10', '2026-01-10');

    -- Sigorta (depo) - Eylül 2025
    INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
    VALUES (gen_random_uuid(), v_cat_diger, v_store_id, NULL, '2025-09-01'::timestamp, 'YEARLY', 'Sigorta (depo)', 6000.00, 20, 1200.00, true, 6000.00, '2025-09-01', '2025-09-01');

    -- =========================================================
    -- TEK SEFERLİK (ONE_TIME) GİDERLER
    -- =========================================================

    -- Influencer iş birliği - Kasım 2025
    INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
    VALUES (gen_random_uuid(), v_cat_reklam, v_store_id, NULL, '2025-11-20'::timestamp, 'ONE_TIME', 'Influencer iş birliği', 5000.00, 20, 1000.00, true, 5000.00, '2025-11-20', '2025-11-20');

    -- Ürün fotoğraf çekimi - Ağustos 2025
    INSERT INTO store_expenses (id, expense_category_id, store_id, product_id, date, frequency, name, amount, vat_rate, vat_amount, is_vat_deductible, net_amount, created_at, updated_at)
    VALUES (gen_random_uuid(), v_cat_diger, v_store_id, NULL, '2025-08-10'::timestamp, 'ONE_TIME', 'Ürün fotoğraf çekimi', 3000.00, 20, 600.00, true, 3000.00, '2025-08-10', '2025-08-10');

    RAISE NOTICE 'Demo gider verileri başarıyla eklendi!';
END $$;
