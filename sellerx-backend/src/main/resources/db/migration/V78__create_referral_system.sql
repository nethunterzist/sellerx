-- Referral/Affiliate system tables and columns

-- Add referral columns to users table
ALTER TABLE users ADD COLUMN referral_code VARCHAR(12) UNIQUE;
ALTER TABLE users ADD COLUMN referred_by_user_id BIGINT REFERENCES users(id);

-- Referral records table - tracks each referral relationship and reward
CREATE TABLE referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_user_id BIGINT NOT NULL REFERENCES users(id),
    referred_user_id BIGINT NOT NULL REFERENCES users(id),
    referral_code VARCHAR(12) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reward_days_granted INTEGER DEFAULT 0,
    reward_applied_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_referral_pair UNIQUE (referrer_user_id, referred_user_id),
    CONSTRAINT chk_no_self_referral CHECK (referrer_user_id != referred_user_id)
);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_user_id);
CREATE INDEX idx_referrals_referred ON referrals(referred_user_id);
CREATE INDEX idx_referrals_status ON referrals(status);
