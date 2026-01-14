CREATE TABLE IF NOT EXISTS stores
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_name    VARCHAR(255) NOT NULL,
    marketplace   VARCHAR(100) NOT NULL,
    credentials   JSONB NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
