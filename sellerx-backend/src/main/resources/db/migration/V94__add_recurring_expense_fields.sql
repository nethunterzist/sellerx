-- Tekrarlayan gider (recurring expense) alanları
ALTER TABLE store_expenses ADD COLUMN end_date TIMESTAMP;
ALTER TABLE store_expenses ADD COLUMN is_recurring_template BOOLEAN DEFAULT FALSE;
ALTER TABLE store_expenses ADD COLUMN parent_expense_id UUID REFERENCES store_expenses(id);
ALTER TABLE store_expenses ADD COLUMN last_generated_date DATE;

-- Index for scheduled job performance
CREATE INDEX idx_store_expenses_recurring_template ON store_expenses(is_recurring_template, frequency)
  WHERE is_recurring_template = TRUE;

-- Comments
COMMENT ON COLUMN store_expenses.end_date IS 'End date for recurring expense template';
COMMENT ON COLUMN store_expenses.is_recurring_template IS 'TRUE if this is a recurring expense template';
COMMENT ON COLUMN store_expenses.parent_expense_id IS 'Links generated instances to their template';
COMMENT ON COLUMN store_expenses.last_generated_date IS 'Last date when instance was generated for this template';
