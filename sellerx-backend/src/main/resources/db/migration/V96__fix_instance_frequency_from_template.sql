-- V96: Fix recurring expense instances to inherit frequency from parent template
-- Problem: Instances were created with frequency = ONE_TIME instead of parent's frequency
-- User feedback: "If I said this expense happens monthly, all records should show monthly"

-- Update instances to inherit frequency from their parent template
UPDATE store_expenses child
SET frequency = parent.frequency
FROM store_expenses parent
WHERE child.parent_expense_id = parent.id
  AND child.frequency = 'ONE_TIME'
  AND parent.frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');
