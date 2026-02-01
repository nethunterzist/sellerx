-- V58: Create education videos, video watch history, and user notifications tables

-- Education Videos Table
CREATE TABLE IF NOT EXISTS education_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    duration VARCHAR(20) NOT NULL,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    video_type VARCHAR(20) NOT NULL,
    video_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for category filtering
CREATE INDEX idx_education_video_category ON education_videos(category);

-- Index for active videos ordered by order
CREATE INDEX idx_education_video_active_order ON education_videos(is_active, video_order);

-- Video Watch History Table
CREATE TABLE IF NOT EXISTS video_watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id UUID NOT NULL REFERENCES education_videos(id) ON DELETE CASCADE,
    watched_at TIMESTAMP NOT NULL DEFAULT NOW(),
    watched_duration INTEGER,
    UNIQUE(user_id, video_id)
);

-- Index for user watch history queries
CREATE INDEX idx_video_watch_user ON video_watch_history(user_id);

-- Index for video watch history queries
CREATE INDEX idx_video_watch_video ON video_watch_history(video_id);

-- User Notifications Table
CREATE TABLE IF NOT EXISTS user_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link TEXT,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for user notifications queries (user, read status, created date)
CREATE INDEX idx_notification_user_read_created ON user_notifications(user_id, read, created_at DESC);
