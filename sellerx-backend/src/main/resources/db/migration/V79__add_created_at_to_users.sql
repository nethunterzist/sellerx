-- Add created_at and last_login_at columns to users table
ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

-- Create indexes for admin dashboard queries
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);
