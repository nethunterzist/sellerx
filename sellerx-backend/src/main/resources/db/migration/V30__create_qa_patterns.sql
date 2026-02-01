-- QA Patterns Table (Seniority Tracking)
-- Soru kalıpları ve kıdem takibi

CREATE TABLE qa_patterns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE NOT NULL,

    -- Pattern tanımlama
    pattern_hash VARCHAR(64) NOT NULL,
    canonical_question TEXT NOT NULL,
    canonical_answer TEXT,

    -- Kıdem metrikleri
    occurrence_count INT NOT NULL DEFAULT 1,
    approval_count INT NOT NULL DEFAULT 0,
    rejection_count INT NOT NULL DEFAULT 0,
    modification_count INT NOT NULL DEFAULT 0,

    -- Güven metrikleri
    confidence_score DECIMAL(5,4) NOT NULL DEFAULT 0.0,
    last_human_review TIMESTAMP,

    -- Auto-submit durumu
    seniority_level VARCHAR(20) NOT NULL DEFAULT 'JUNIOR',
    is_auto_submit_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    auto_submit_enabled_at TIMESTAMP,
    auto_submit_disabled_reason TEXT,

    -- İlişkili ürün/kategori
    product_id VARCHAR(255),
    category VARCHAR(255),

    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pattern_store_hash UNIQUE(store_id, pattern_hash)
);

-- Indexes
CREATE INDEX idx_patterns_store_seniority ON qa_patterns(store_id, seniority_level);
CREATE INDEX idx_patterns_auto_submit ON qa_patterns(store_id, is_auto_submit_eligible) WHERE is_auto_submit_eligible = TRUE;
CREATE INDEX idx_patterns_store_hash ON qa_patterns(store_id, pattern_hash);
CREATE INDEX idx_patterns_confidence ON qa_patterns(store_id, confidence_score DESC);

-- Comments
COMMENT ON TABLE qa_patterns IS 'Soru kalıpları ve kıdem takibi';
COMMENT ON COLUMN qa_patterns.seniority_level IS 'JUNIOR, LEARNING, SENIOR, EXPERT';
COMMENT ON COLUMN qa_patterns.pattern_hash IS 'MinHash benzeri hash değeri';
COMMENT ON COLUMN qa_patterns.confidence_score IS '0.0-1.0 arası güven skoru';
