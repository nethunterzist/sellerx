-- Add EMAIL_VERIFICATION template
INSERT INTO email_templates (email_type, name, subject_template, body_template, description) VALUES
('EMAIL_VERIFICATION', 'Email Doğrulama',
 'Email Adresinizi Doğrulayın',
 '<h2>Email Adresinizi Doğrulayın</h2>
<p>Merhaba {{userName}},</p>
<p>SellerX hesabınızı oluşturduğunuz için teşekkür ederiz. Hesabınızın güvenliği için lütfen email adresinizi doğrulayın.</p>
<p style="text-align: center;">
    <a href="{{verificationLink}}" class="button">Email Adresimi Doğrula</a>
</p>
<p>Veya aşağıdaki linki tarayıcınıza kopyalayın:</p>
<p style="word-break: break-all; color: #6b7280; font-size: 14px;">{{verificationLink}}</p>
<p style="color: #6b7280; font-size: 14px; margin-top: 24px;">Bu link 24 saat geçerlidir. Eğer bu talebi siz yapmadıysanız, bu emaili görmezden gelebilirsiniz.</p>',
 'Kayıt sonrası email doğrulama');
