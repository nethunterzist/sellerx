-- Q&A Module: Customer Questions and Answers
-- Trendyol müşteri soruları ve satıcı cevapları

-- Müşteri Soruları Tablosu
CREATE TABLE trendyol_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    question_id VARCHAR(255) NOT NULL,          -- Trendyol'daki soru ID'si
    product_id VARCHAR(255),                     -- Trendyol product ID
    barcode VARCHAR(100),                        -- Ürün barkodu
    product_title VARCHAR(500),                  -- Ürün başlığı
    customer_question TEXT NOT NULL,             -- Müşteri sorusu
    question_date TIMESTAMP NOT NULL,            -- Sorunun sorulduğu tarih
    status VARCHAR(50) DEFAULT 'PENDING',        -- PENDING, ANSWERED
    is_public BOOLEAN DEFAULT true,              -- Herkese açık mı?
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, question_id)
);

-- Cevaplar Tablosu
CREATE TABLE trendyol_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES trendyol_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,                   -- Cevap metni
    is_submitted BOOLEAN DEFAULT false,          -- Trendyol'a gönderildi mi?
    trendyol_answer_id VARCHAR(255),             -- Trendyol'dan dönen cevap ID'si
    submitted_at TIMESTAMP,                       -- Gönderim tarihi
    submitted_by BIGINT REFERENCES users(id),      -- Cevabı yazan kullanıcı
    created_at TIMESTAMP DEFAULT NOW()
);

-- Performans indexleri
CREATE INDEX idx_questions_store_status ON trendyol_questions(store_id, status);
CREATE INDEX idx_questions_store_date ON trendyol_questions(store_id, question_date DESC);
CREATE INDEX idx_questions_product ON trendyol_questions(product_id);
CREATE INDEX idx_answers_question ON trendyol_answers(question_id);

-- Yorum: Bu tablolar Trendyol Q&A API entegrasyonu için kullanılacak
-- GET /integration/suppliers/{supplierId}/questions - Soruları çek
-- POST /integration/suppliers/{supplierId}/questions/{questionId}/answers - Cevap gönder
