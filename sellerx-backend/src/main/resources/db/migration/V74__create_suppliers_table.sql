-- Supplier entity for managing vendors/suppliers
CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(100),
    address TEXT,
    country VARCHAR(100),
    currency VARCHAR(3) DEFAULT 'TRY',
    payment_terms_days INTEGER,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(store_id, name)
);

CREATE INDEX idx_suppliers_store_id ON suppliers(store_id);
