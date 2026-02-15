-- Add default expense categories to stores that have no categories
-- This ensures all stores have basic expense tracking capabilities

INSERT INTO expense_categories (id, store_id, name, created_at, updated_at)
SELECT
    gen_random_uuid(),
    s.id,
    cat.name,
    NOW(),
    NOW()
FROM stores s
CROSS JOIN (
    VALUES ('Ambalaj'), ('Kargo'), ('Reklam'), ('Ofis'), ('Muhasebe'), ('Diğer')
) AS cat(name)
WHERE NOT EXISTS (
    SELECT 1 FROM expense_categories ec WHERE ec.store_id = s.id
);
