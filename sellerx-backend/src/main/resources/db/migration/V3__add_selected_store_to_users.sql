-- Add selected_store_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS selected_store_id UUID;

-- Add foreign key constraint
ALTER TABLE users ADD CONSTRAINT fk_users_selected_store 
    FOREIGN KEY (selected_store_id) REFERENCES stores(id);
