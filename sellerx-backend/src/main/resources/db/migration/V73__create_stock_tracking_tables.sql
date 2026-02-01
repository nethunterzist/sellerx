-- Stock Tracking Tables for Competitor Stock Monitoring
-- Similar to Buybox tracking but for stock levels instead of prices

-- Table 1: Tracked Products (max 10 per store)
CREATE TABLE stock_tracked_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Trendyol product info (competitor's product)
    trendyol_product_id BIGINT NOT NULL,
    product_url VARCHAR(500) NOT NULL,
    product_name VARCHAR(500),
    brand_name VARCHAR(200),
    image_url VARCHAR(500),

    -- Tracking settings
    check_interval_hours INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT true,

    -- Alert settings
    alert_on_out_of_stock BOOLEAN DEFAULT true,
    alert_on_low_stock BOOLEAN DEFAULT true,
    low_stock_threshold INTEGER DEFAULT 10,
    alert_on_stock_increase BOOLEAN DEFAULT false,
    alert_on_back_in_stock BOOLEAN DEFAULT true,

    -- Current state & meta
    last_checked_at TIMESTAMP,
    last_stock_quantity INTEGER,
    last_price DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uq_stock_tracked_store_product UNIQUE(store_id, trendyol_product_id)
);

CREATE INDEX idx_stock_tracked_active ON stock_tracked_products(is_active, last_checked_at);
CREATE INDEX idx_stock_tracked_store ON stock_tracked_products(store_id);
CREATE INDEX idx_stock_tracked_needs_check ON stock_tracked_products(is_active, last_checked_at)
    WHERE is_active = true;

-- Table 2: Stock Snapshots (historical data)
CREATE TABLE stock_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracked_product_id UUID NOT NULL REFERENCES stock_tracked_products(id) ON DELETE CASCADE,

    -- Stock data
    quantity INTEGER NOT NULL,
    in_stock BOOLEAN NOT NULL,
    price DECIMAL(15,2),

    -- Change tracking
    previous_quantity INTEGER,
    quantity_change INTEGER,

    checked_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_stock_snapshots_product ON stock_snapshots(tracked_product_id, checked_at DESC);

-- Table 3: Stock Alerts
CREATE TABLE stock_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracked_product_id UUID NOT NULL REFERENCES stock_tracked_products(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,

    -- Alert details
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    title VARCHAR(500) NOT NULL,
    message TEXT,

    -- Stock data at alert time
    old_quantity INTEGER,
    new_quantity INTEGER,
    threshold INTEGER,

    -- Notification status
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    email_sent BOOLEAN DEFAULT false,

    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_stock_alerts_store ON stock_alerts(store_id, created_at DESC);
CREATE INDEX idx_stock_alerts_unread ON stock_alerts(store_id, is_read) WHERE is_read = false;
CREATE INDEX idx_stock_alerts_product ON stock_alerts(tracked_product_id, created_at DESC);

-- Comments for documentation
COMMENT ON TABLE stock_tracked_products IS 'Products from competitors being tracked for stock changes';
COMMENT ON TABLE stock_snapshots IS 'Historical stock levels captured every hour';
COMMENT ON TABLE stock_alerts IS 'Alerts triggered by stock changes';
COMMENT ON COLUMN stock_tracked_products.check_interval_hours IS 'How often to check stock (default: 1 hour)';
COMMENT ON COLUMN stock_tracked_products.low_stock_threshold IS 'Alert when stock falls below this number';
