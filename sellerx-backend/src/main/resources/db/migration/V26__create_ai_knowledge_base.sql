-- V25: AI Knowledge Base ve ilgili tablolar
-- GPT-4 AI entegrasyonu için gerekli tablolar

-- Mağaza Bilgi Bankası (Store Knowledge Base)
CREATE TABLE store_knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    keywords TEXT[],
    is_active BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- AI Cevap Şablonları
CREATE TABLE answer_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    template_text TEXT NOT NULL,
    category VARCHAR(100),
    variables TEXT[],
    usage_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- AI Ayarları (Mağaza Bazlı)
CREATE TABLE store_ai_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id) ON DELETE CASCADE,
    ai_enabled BOOLEAN DEFAULT false,
    auto_answer BOOLEAN DEFAULT false,
    tone VARCHAR(50) DEFAULT 'professional',
    language VARCHAR(10) DEFAULT 'tr',
    max_answer_length INTEGER DEFAULT 500,
    include_greeting BOOLEAN DEFAULT true,
    include_signature BOOLEAN DEFAULT true,
    signature_text VARCHAR(255),
    confidence_threshold DECIMAL(3,2) DEFAULT 0.80,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- AI Cevap Logları (Audit)
CREATE TABLE ai_answer_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES trendyol_questions(id) ON DELETE CASCADE,
    generated_answer TEXT NOT NULL,
    confidence_score DECIMAL(3,2),
    context_used JSONB,
    model_version VARCHAR(50),
    tokens_used INTEGER,
    generation_time_ms INTEGER,
    was_approved BOOLEAN,
    was_edited BOOLEAN,
    final_answer TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexler
CREATE INDEX idx_knowledge_store_category ON store_knowledge_base(store_id, category);
CREATE INDEX idx_knowledge_keywords ON store_knowledge_base USING GIN(keywords);
CREATE INDEX idx_knowledge_active ON store_knowledge_base(store_id, is_active);
CREATE INDEX idx_templates_store ON answer_templates(store_id);
CREATE INDEX idx_templates_category ON answer_templates(store_id, category);
CREATE INDEX idx_ai_logs_question ON ai_answer_logs(question_id);
CREATE INDEX idx_ai_logs_created ON ai_answer_logs(created_at DESC);
