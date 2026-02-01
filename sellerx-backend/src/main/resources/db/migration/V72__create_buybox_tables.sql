-- =====================================================
-- V72: Buybox Tracking System Tables
-- =====================================================
-- Müşterilerin kendi envanterlerinden seçtikleri ürünlerin
-- buybox durumunu takip etmelerini sağlayan sistem.
-- Kontrol sıklığı: 12 saat, Maksimum ürün: 10/mağaza
-- =====================================================

-- Takip edilen ürünler
CREATE TABLE buybox_tracked_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES trendyol_products(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT TRUE,
    alert_on_loss BOOLEAN DEFAULT TRUE,
    alert_on_new_competitor BOOLEAN DEFAULT TRUE,
    alert_price_threshold DECIMAL(10,2) DEFAULT 10.00,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, product_id)
);

-- Buybox anlık görüntüleri (12 saatlik kontroller)
CREATE TABLE buybox_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracked_product_id UUID NOT NULL REFERENCES buybox_tracked_products(id) ON DELETE CASCADE,
    checked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    buybox_status VARCHAR(20) NOT NULL, -- WON, LOST, RISK, NO_COMPETITION
    winner_merchant_id BIGINT,
    winner_merchant_name VARCHAR(255),
    winner_price DECIMAL(10,2),
    winner_seller_score DECIMAL(3,1),
    my_price DECIMAL(10,2),
    my_position INTEGER, -- 1=winner, 2,3,4...
    price_difference DECIMAL(10,2), -- pozitif = sen pahalısın
    total_sellers INTEGER,
    lowest_price DECIMAL(10,2),
    highest_price DECIMAL(10,2),
    competitors_json JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Buybox alertleri
CREATE TABLE buybox_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    tracked_product_id UUID NOT NULL REFERENCES buybox_tracked_products(id) ON DELETE CASCADE,
    alert_type VARCHAR(30) NOT NULL, -- BUYBOX_LOST, BUYBOX_WON, NEW_COMPETITOR, PRICE_RISK
    title VARCHAR(255),
    message TEXT,
    old_winner_name VARCHAR(255),
    new_winner_name VARCHAR(255),
    price_before DECIMAL(10,2),
    price_after DECIMAL(10,2),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    read_at TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_buybox_tracked_store ON buybox_tracked_products(store_id);
CREATE INDEX idx_buybox_tracked_active ON buybox_tracked_products(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_buybox_snapshots_tracked ON buybox_snapshots(tracked_product_id);
CREATE INDEX idx_buybox_snapshots_checked ON buybox_snapshots(checked_at);
CREATE INDEX idx_buybox_alerts_store ON buybox_alerts(store_id);
CREATE INDEX idx_buybox_alerts_unread ON buybox_alerts(store_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_buybox_alerts_created ON buybox_alerts(created_at DESC);
