-- Knowledge Suggestions Table
-- AI tarafından keşfedilen bilgi kalıpları ve öneriler

CREATE TABLE knowledge_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE NOT NULL,

    -- Pattern bilgileri
    suggested_title VARCHAR(255) NOT NULL,
    suggested_content TEXT NOT NULL,
    sample_questions JSONB NOT NULL DEFAULT '[]',

    -- Metrikler
    question_count INT NOT NULL DEFAULT 1,
    avg_similarity DECIMAL(5,4) DEFAULT 0.0,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Durum
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',

    -- İnceleme
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id),
    review_notes TEXT,
    created_knowledge_id UUID,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_suggestions_store_status ON knowledge_suggestions(store_id, status);
CREATE INDEX idx_suggestions_priority ON knowledge_suggestions(store_id, priority DESC, created_at DESC);
CREATE INDEX idx_suggestions_pending ON knowledge_suggestions(store_id) WHERE status = 'PENDING';

-- Comments
COMMENT ON TABLE knowledge_suggestions IS 'AI tarafından keşfedilen bilgi kalıpları ve öneriler';
COMMENT ON COLUMN knowledge_suggestions.status IS 'PENDING, ACCEPTED, REJECTED, MODIFIED';
COMMENT ON COLUMN knowledge_suggestions.priority IS 'LOW, MEDIUM, HIGH';
COMMENT ON COLUMN knowledge_suggestions.sample_questions IS 'İlk 5 örnek soru JSON array olarak';
