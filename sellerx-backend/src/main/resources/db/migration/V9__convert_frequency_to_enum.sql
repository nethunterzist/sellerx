-- Create expense_frequency enum type
CREATE TYPE expense_frequency AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ONE_TIME');

-- Alter table to use the enum type
ALTER TABLE store_expenses 
DROP CONSTRAINT check_frequency,
ALTER COLUMN frequency TYPE expense_frequency USING frequency::expense_frequency;
