-- Remove buybox feature tables
-- This migration drops all buybox-related tables as the feature is being removed

-- Drop dependent tables first (due to foreign keys)
DROP TABLE IF EXISTS buybox_alerts CASCADE;
DROP TABLE IF EXISTS buybox_snapshots CASCADE;
DROP TABLE IF EXISTS buybox_tracked_products CASCADE;
