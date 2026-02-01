-- V27: Add preferences JSONB column to users table
-- Stores user preferences including language, theme, currency, and notification settings

ALTER TABLE users ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{
  "language": "tr",
  "theme": "light",
  "currency": "TRY",
  "notifications": {
    "email": true,
    "push": true,
    "orderUpdates": true,
    "stockAlerts": true,
    "weeklyReport": false
  }
}'::jsonb;

-- Index for efficient currency-based queries
CREATE INDEX IF NOT EXISTS idx_users_preferences_currency ON users ((preferences->>'currency'));

-- Index for efficient theme-based queries (dark mode users count etc.)
CREATE INDEX IF NOT EXISTS idx_users_preferences_theme ON users ((preferences->>'theme'));

COMMENT ON COLUMN users.preferences IS 'User preferences stored as JSONB: language (tr/en), theme (light/dark/system), currency (TRY/USD/EUR), notifications';
