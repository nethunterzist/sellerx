-- Add email verification fields to users table

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(64);
ALTER TABLE users ADD COLUMN email_verification_sent_at TIMESTAMP WITH TIME ZONE;

-- Index for token lookup
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token) WHERE email_verification_token IS NOT NULL;

COMMENT ON COLUMN users.email_verified IS 'Whether user has verified their email address';
COMMENT ON COLUMN users.email_verification_token IS 'Token sent to user for email verification';
COMMENT ON COLUMN users.email_verification_sent_at IS 'When the verification email was last sent';
