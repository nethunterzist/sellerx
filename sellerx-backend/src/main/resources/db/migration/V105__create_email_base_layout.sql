-- Email base layout for consistent email appearance
-- Single record table - only one base layout exists
CREATE TABLE email_base_layout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    header_html TEXT NOT NULL,
    footer_html TEXT NOT NULL,
    styles TEXT,
    logo_url VARCHAR(500),
    primary_color VARCHAR(20) NOT NULL DEFAULT '#2563eb',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE email_base_layout IS 'Base layout for all email templates - single record';
COMMENT ON COLUMN email_base_layout.header_html IS 'HTML content for email header (logo, branding)';
COMMENT ON COLUMN email_base_layout.footer_html IS 'HTML content for email footer (copyright, links)';
COMMENT ON COLUMN email_base_layout.styles IS 'CSS styles for the email template';
COMMENT ON COLUMN email_base_layout.logo_url IS 'URL of the logo image';
COMMENT ON COLUMN email_base_layout.primary_color IS 'Primary brand color (hex)';

-- Insert default base layout
INSERT INTO email_base_layout (header_html, footer_html, styles, logo_url, primary_color) VALUES (
    -- Header
    '<div style="text-align: center; padding: 24px 0; border-bottom: 1px solid #e5e7eb;">
        <img src="{{logoUrl}}" alt="SellerX" style="height: 40px;" />
    </div>',
    -- Footer
    '<div style="margin-top: 32px; padding-top: 16px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 12px; text-align: center;">
        <p>Bu email SellerX tarafından otomatik olarak gönderilmiştir.</p>
        <p style="margin-top: 8px;">
            <a href="https://sellerx.com/yardim" style="color: #6b7280;">Yardım</a> |
            <a href="https://sellerx.com/abonelik" style="color: #6b7280;">Abonelik Yönetimi</a>
        </p>
        <p style="margin-top: 8px;">&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
    </div>',
    -- Styles
    'body {
        font-family: -apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif;
        line-height: 1.6;
        color: #1f2937;
        background-color: #f3f4f6;
        margin: 0;
        padding: 20px;
    }
    .container {
        max-width: 600px;
        margin: 0 auto;
        background-color: #ffffff;
        border-radius: 8px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        padding: 32px;
    }
    h1, h2 { color: #111827; margin-top: 0; }
    a { color: {{primaryColor}}; }
    .button {
        display: inline-block;
        background-color: {{primaryColor}};
        color: #ffffff !important;
        text-decoration: none;
        padding: 12px 24px;
        border-radius: 6px;
        margin: 16px 0;
    }
    .content {
        padding: 24px 0;
    }',
    -- Logo URL (placeholder - admin will update)
    'https://sellerx.com/logo.png',
    -- Primary color
    '#2563eb'
);
