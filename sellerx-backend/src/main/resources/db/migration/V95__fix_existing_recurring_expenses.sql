-- V95: Fix existing recurring expenses to be marked as templates
-- Problem: V94 added is_recurring_template column with DEFAULT FALSE
-- but did not update existing DAILY/WEEKLY/MONTHLY/YEARLY expenses to TRUE

-- Mark all existing recurring expenses (non-ONE_TIME) as templates
-- Only update those that are not already generated instances (parent_expense_id IS NULL)
UPDATE store_expenses
SET is_recurring_template = true
WHERE frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')
  AND is_recurring_template = false
  AND parent_expense_id IS NULL;
