-- Impersonation audit log table
CREATE TABLE impersonation_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL REFERENCES users(id),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_impersonation_logs_admin ON impersonation_logs(admin_user_id);
CREATE INDEX idx_impersonation_logs_target ON impersonation_logs(target_user_id);
CREATE INDEX idx_impersonation_logs_created ON impersonation_logs(created_at DESC);
