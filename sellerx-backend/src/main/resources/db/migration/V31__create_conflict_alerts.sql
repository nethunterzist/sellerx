-- Conflict Alerts Table
-- Çelişki ve risk uyarıları

CREATE TABLE conflict_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE NOT NULL,
    question_id UUID REFERENCES trendyol_questions(id) ON DELETE SET NULL,

    -- Çelişki detayları
    conflict_type VARCHAR(30) NOT NULL,
    severity VARCHAR(10) NOT NULL,

    -- Çelişen veriler
    source_a_type VARCHAR(50),
    source_a_content TEXT,
    source_b_type VARCHAR(50),
    source_b_content TEXT,

    -- Tespit edilen anahtar kelimeler
    detected_keywords JSONB DEFAULT '[]',

    -- Durum
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    resolution_notes TEXT,
    resolved_at TIMESTAMP,
    resolved_by BIGINT REFERENCES users(id),

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_conflicts_store_status ON conflict_alerts(store_id, status, severity DESC);
CREATE INDEX idx_conflicts_question ON conflict_alerts(question_id) WHERE question_id IS NOT NULL;
CREATE INDEX idx_conflicts_active ON conflict_alerts(store_id) WHERE status = 'ACTIVE';

-- Comments
COMMENT ON TABLE conflict_alerts IS 'Çelişki ve risk uyarıları';
COMMENT ON COLUMN conflict_alerts.conflict_type IS 'KNOWLEDGE_VS_TRENDYOL, BRAND_INCONSISTENCY, LEGAL_RISK, HEALTH_SAFETY';
COMMENT ON COLUMN conflict_alerts.severity IS 'LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN conflict_alerts.status IS 'ACTIVE, RESOLVED, DISMISSED';
COMMENT ON COLUMN conflict_alerts.source_a_type IS 'KNOWLEDGE_BASE, TRENDYOL_DATA, BRAND_INFO';
