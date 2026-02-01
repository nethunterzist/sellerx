-- Trendyol Claims table for Returns Management
CREATE TABLE trendyol_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id),
    claim_id VARCHAR(100) NOT NULL,
    order_number VARCHAR(50),
    customer_first_name VARCHAR(100),
    customer_last_name VARCHAR(100),
    claim_date TIMESTAMP,
    cargo_tracking_number VARCHAR(100),
    cargo_tracking_link VARCHAR(500),
    cargo_provider_name VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    items JSONB,
    last_modified_date TIMESTAMP,
    synced_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, claim_id)
);

-- Indexes for performance
CREATE INDEX idx_claims_store_status ON trendyol_claims(store_id, status);
CREATE INDEX idx_claims_claim_date ON trendyol_claims(claim_date);
CREATE INDEX idx_claims_order_number ON trendyol_claims(order_number);
