-- Drop the existing foreign key constraint
ALTER TABLE users DROP CONSTRAINT fk_users_selected_store;

-- Add new foreign key constraint with ON DELETE SET NULL
ALTER TABLE users ADD CONSTRAINT fk_users_selected_store 
    FOREIGN KEY (selected_store_id) REFERENCES stores(id) ON DELETE SET NULL;
