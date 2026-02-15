-- Email templates for dynamic email content
CREATE TABLE email_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_type VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    subject_template VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for quick lookup
CREATE INDEX idx_email_templates_type ON email_templates(email_type);
CREATE INDEX idx_email_templates_active ON email_templates(is_active) WHERE is_active = true;

COMMENT ON TABLE email_templates IS 'Email templates with variable placeholders like {{userName}}';
COMMENT ON COLUMN email_templates.subject_template IS 'Subject with placeholders: "Merhaba {{userName}}, hoş geldiniz!"';
COMMENT ON COLUMN email_templates.body_template IS 'HTML body with placeholders';

-- Insert default templates (placeholder content - user will customize)
INSERT INTO email_templates (email_type, name, subject_template, body_template, description) VALUES
('WELCOME', 'Hoş Geldiniz', 'SellerX''e Hoş Geldiniz!', '<p>Merhaba {{userName}},</p><p>SellerX ailesine hoş geldiniz!</p>', 'Kullanıcı kayıt olduğunda gönderilir'),
('PASSWORD_RESET', 'Şifre Sıfırlama', 'Şifre Sıfırlama Talebi', '<p>Merhaba {{userName}},</p><p>Şifrenizi sıfırlamak için <a href="{{resetLink}}">buraya tıklayın</a>.</p>', 'Şifre sıfırlama talep edildiğinde'),
('SUBSCRIPTION_CONFIRMED', 'Abonelik Onayı', 'Aboneliğiniz Aktif!', '<p>Merhaba {{userName}},</p><p>{{planName}} planınız aktif edildi.</p>', 'Abonelik satın alındığında'),
('SUBSCRIPTION_REMINDER_7', 'Abonelik Hatırlatma (7 gün)', 'Aboneliğiniz 7 Gün İçinde Sona Eriyor', '<p>Merhaba {{userName}},</p><p>{{planName}} aboneliğiniz {{expiryDate}} tarihinde sona erecek.</p>', '7 gün kala hatırlatma'),
('SUBSCRIPTION_REMINDER_1', 'Abonelik Hatırlatma (1 gün)', 'Aboneliğiniz Yarın Sona Eriyor!', '<p>Merhaba {{userName}},</p><p>{{planName}} aboneliğiniz yarın sona erecek.</p>', '1 gün kala son uyarı'),
('SUBSCRIPTION_RENEWED', 'Abonelik Yenilendi', 'Aboneliğiniz Yenilendi', '<p>Merhaba {{userName}},</p><p>{{planName}} aboneliğiniz başarıyla yenilendi.</p>', 'Otomatik yenileme sonrası'),
('PAYMENT_FAILED', 'Ödeme Başarısız', 'Ödemeniz Alınamadı', '<p>Merhaba {{userName}},</p><p>Ödemeniz alınamadı. Lütfen ödeme bilgilerinizi güncelleyin.</p>', 'Ödeme başarısız olduğunda'),
('SUBSCRIPTION_CANCELLED', 'Abonelik İptal', 'Aboneliğiniz İptal Edildi', '<p>Merhaba {{userName}},</p><p>{{planName}} aboneliğiniz iptal edildi.</p>', 'Kullanıcı iptal ettiğinde'),
('ALERT_NOTIFICATION', 'Alarm Bildirimi', '{{alertTitle}}', '<p>Merhaba {{userName}},</p><p>{{alertMessage}}</p>', 'Alarm tetiklendiğinde'),
('DAILY_DIGEST', 'Günlük Özet', 'Günlük Özet - {{date}}', '<p>Merhaba {{userName}},</p><p>{{digestContent}}</p>', 'Günlük özet raporu'),
('WEEKLY_REPORT', 'Haftalık Rapor', 'Haftalık Performans Raporu', '<p>Merhaba {{userName}},</p><p>{{reportContent}}</p>', 'Haftalık performans özeti'),
('ADMIN_BROADCAST', 'Admin Duyurusu', '{{subject}}', '{{content}}', 'Admin tarafından gönderilen duyurular');
