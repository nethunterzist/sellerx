-- Migration to add new fields to trendyol_products table
-- Add trendyol_quantity column by renaming quantity
ALTER TABLE trendyol_products 
RENAME COLUMN quantity TO trendyol_quantity;

-- Add new status fields
ALTER TABLE trendyol_products 
ADD COLUMN IF NOT EXISTS approved BOOLEAN DEFAULT false;

ALTER TABLE trendyol_products 
ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT false;

ALTER TABLE trendyol_products 
ADD COLUMN IF NOT EXISTS blacklisted BOOLEAN DEFAULT false;

ALTER TABLE trendyol_products 
ADD COLUMN IF NOT EXISTS rejected BOOLEAN DEFAULT false;

ALTER TABLE trendyol_products 
ADD COLUMN IF NOT EXISTS on_sale BOOLEAN DEFAULT false;
