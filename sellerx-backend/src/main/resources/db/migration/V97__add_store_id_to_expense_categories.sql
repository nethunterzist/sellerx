-- V97: Add store_id to expense_categories for store-specific categories
-- Previously categories were global (shared by all users)
-- Now each store has its own categories that can be managed independently

-- Step 1: Add store_id column (nullable initially for migration)
ALTER TABLE expense_categories ADD COLUMN store_id UUID;

-- Step 2: Drop the unique constraint on name (will be replaced with composite unique)
ALTER TABLE expense_categories DROP CONSTRAINT IF EXISTS expense_categories_name_key;

-- Step 3: Create store-specific categories for each existing store
-- Copy the 6 default categories to each store
INSERT INTO expense_categories (name, store_id, created_at, updated_at)
SELECT ec.name, s.id, NOW(), NOW()
FROM expense_categories ec
CROSS JOIN stores s
WHERE ec.store_id IS NULL;

-- Step 4: Update store_expenses to point to new store-specific categories
-- Using PostgreSQL UPDATE...FROM syntax for proper join support
UPDATE store_expenses se
SET expense_category_id = new_cat.id
FROM expense_categories old_cat
JOIN expense_categories new_cat ON old_cat.name = new_cat.name
WHERE old_cat.id = se.expense_category_id
  AND old_cat.store_id IS NULL
  AND new_cat.store_id = se.store_id;

-- Step 5: Delete old global categories (those without store_id)
DELETE FROM expense_categories WHERE store_id IS NULL;

-- Step 6: Make store_id NOT NULL now that all categories have a store
ALTER TABLE expense_categories ALTER COLUMN store_id SET NOT NULL;

-- Step 7: Add foreign key constraint to stores table
ALTER TABLE expense_categories
ADD CONSTRAINT fk_expense_categories_store
FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

-- Step 8: Add unique constraint for (store_id, name)
-- Same category name cannot exist twice in the same store
ALTER TABLE expense_categories
ADD CONSTRAINT uk_expense_categories_store_name UNIQUE (store_id, name);

-- Step 9: Add index for store_id lookups
CREATE INDEX idx_expense_categories_store_id ON expense_categories(store_id);
