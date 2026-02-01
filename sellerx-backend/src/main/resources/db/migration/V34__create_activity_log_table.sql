-- Activity logs for tracking user login/logout events
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    email VARCHAR(255),
    action VARCHAR(50) NOT NULL,  -- login, logout, failed_login
    device VARCHAR(20),
    browser VARCHAR(100),
    ip_address VARCHAR(50),
    user_agent TEXT,
    success BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);
